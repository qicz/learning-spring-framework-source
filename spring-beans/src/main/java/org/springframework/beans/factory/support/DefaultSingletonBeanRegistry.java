/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.izcqi.learning.utils.LogUtil;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Cache of singleton objects: bean name to bean instance. */
	/**
	 * Spring中所有bean的缓存池，<b>一级缓存</b>
	 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	/**
	 * 缓存bean创建过程中的Factory，临时使用，使用完成之后，会清除已使用不需要的ObjectFactory。<br/>
	 * <b>二级缓存</b>
	 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	/**
	 * <b>三级缓存</b>
	 * 其作用： 在创建bean的过程中消耗资源较多，防止bean的重复创建。<br/>
	 * 因为singleFactories中工厂的工作是调用多个postprocessor，消耗很大。<br/>
	 * 尤其在循环依赖中，这样会更利于*原对象*（A依赖B，A就是原对象）的缓存。<br/>
	 * 用完之后，再从singletonFactories移除。
	 */
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

	/** Set of registered singletons, containing the bean names in registration order. */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/** Names of beans that are currently in creation. */
	/**
	当前正在创建的bean的缓存，避免重复创建
	 */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** Names of beans currently excluded from in creation checks. */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** List of suppressed Exceptions, available for associating related causes. */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons. */
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name to disposable instance. */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** Map between dependent bean names: bean name to Set of dependent bean names. */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				//将创建的临时对象放到singletonFactories中，注意此时还不是bean
				this.singletonFactories.put(beanName, singletonFactory);
				// singletonFactories 与 singletonFactories 是互换关系
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	/**
	 * 在getSingletonBean时，
	 * 首先spring会去单例池{@link DefaultSingletonBeanRegistry#singletonObjects}去根据名字获取这个bean，单例池就是一个map
	 * 如果对象被创建了则直接从map中拿出来并且返回
	 * 由于循环引用需要在创建bean的过程中去获取被引用的那个类
	 * 而被引用的这个类如果没有创建，则会调用createBean来创建这个bean
	 * 在创建这个被引用的bean的过程中会判断这个bean的对象有没有实例化
	 * 最后把这个对象put到单例池{@link DefaultSingletonBeanRegistry#singletonObjects}，交给Spring管理，才能算一个bean
	 * 简而言之就是spring先new一个对象，继而对这个对象进行生命周期回调
	 * 接着对这个对象进行属性{@link AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)}填充，也是大家说的自动注入
	 * 然后在进行AOP判断等等；这一些操作简称----spring生命周期
	 * 所以一个bean是一个经历了spring周期的对象，和一个对象有区别
	 *
	 * 循环引用时，首先spring扫描到一个需要被实例化的类A
	 * 于是spring就去创建A；A=new A-a;new A的过程会调用getBean("a"))；
	 *
	 * 所谓的getBean方法--核心也就是这个getSingleton(beanName)
	 *
	 * 如果getA等于空；spring就会实例化A；也就是上面的new A
	 * 但是在实例化A的时候会再次调用一下
	 * getSingleton(String beanName, ObjectFactory<?> singletonFactory)
	 * 笔者上面说过现在写的注释给getSingleton(beanName)
	 * 也即是第一次调用getSingleton(beanName)
	 * 实例化一共会调用两次getSingleton方法；但是是重载
	 * 第二次调用getSingleton方法的时候spring会在一个set集合{@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation}当中记录一下这个类正在被创建
	 * 这个一定要记住，在调用完成第一次getSingleton完成之后
	 * spring判读这个类没有创建，然后调用第二次getSingleton
	 * 在第二次getSingleton里面记录了一下自己已经开始实例化这个类
	 *
	 * 需要说明的spring实例化一个对象底层用的是反射；
	 *
	 * spring实例化一个对象的过程非常复杂，需要推断构造方法等等；
	 *
	 * 这个时候对象a仅仅是一个对象，还不是一个完整的bean
	 * 接着让这个对象去完成spring的bean的生命周期
	 * 过程中spring会判断容器是否允许循环引用
	 * 如果允许循环依赖（Spring默认允许循环依赖{@link AbstractAutowireCapableBeanFactory#allowCircularReferences}），
	 * spring会把这个对象(还不是bean)临时存起来，放到一个map{@link DefaultSingletonBeanRegistry#singletonFactories}当中
	 * 注意这个map和单例池是两个map，在spring源码中单例池的map叫做 singletonObjects
	 * 而这个存放临时对象的map叫做singletonFactories
	 * 当然spring还有一个存放临时对象的map叫做earlySingletonObjects
	 * 所以一共是三个map，也可以成为三级缓存
	 * 为什么需要三个map呢？先来了解这三个map到底都缓存了什么
	 * 第一个map singletonObjects 存放的单例的bean
	 * 第二个map singletonFactories 存放的临时对象(没有完整springBean生命周期的对象)
	 * 第三个map earlySingletonObjects 存放的临时对象(没有完整springBean生命周期的对象)
	 *
	 * 第一个map主要为了直接缓存创建好的bean；方便程序员去getBean；很好理解
	 * 第二个和第三个主要为了循环引用；
	 *
	 * 把对象a缓存到第二个map之后，会接着完善生命周期；
	 * 当进行到对象a的属性填充的这一周期的时候，发觉a依赖了一个B类
	 * 所以spring就会去判断这个B类到底有没有bean在容器当中
	 * 这里的判断就是从第一个map即单例池当中去拿一个bean
	 * 假设没有，那么spring会先去调用createBean创建这个bean
	 *
	 * 于是又回到和创建A一样的流程，在创建B的时候同样也会去getBean("B")；
	 *
	 * 第一次调用完getSingleton完成之后会调用第二次getSingleton
	 * 第二次调用getSingleton同样会在set集合{@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation}当中去记录B正在被创建
	 * 在这个时候 这个set集合至少有两个记录了 A和B；
	 * 如果为空就 b=new B()；创建一个b对象；
	 * 创建完B的对象之后，接着完善B的生命周期
	 * 同样也会判断是否允许循环依赖，如果允许则把对象b存到第二个map当中；
	 * 这个时候第二个map{@link DefaultSingletonBeanRegistry#singletonFactories}当中至少有两个对象了，a和b
	 * 接着继续生命周期；当进行到b对象的属性填充的时候发觉b需要依赖A
	 * 于是就去容器看看A有没有创建，就是从第一个map当中去找a，
	 * 那A是不是就是前面创建的a呢？注意那只是个对象，不是bean，
	 * 还不在第一个map{@link DefaultSingletonBeanRegistry#singletonObjects}当中
	 * 所以b判定A没有创建，于是就是去创建A；
	 *
	 * 那么又再次回到了原点了
	 *
	 * 创建A的过程中；首先调用getBean("a")
	 * 上文说到getBean("a")的核心就是 getSingleton(beanName)
	 * 上文也说了get出来a==null；但是这次却不等于空了
	 * 这次能拿出一个a对象；注意是对象不是bean
	 * 为什么两次不同？原因在于getSingleton(beanName)的源码
	 * getSingleton(beanName)首先从第一个map当中获取bean
	 * 这里就是获取a；但是获取不到；然后判断a是不是等于空
	 * 如果等于空则在判断a是不是正在创建？什么叫做正在创建？
	 * 就是判断a那个set集合{@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation}当中有没有记录A；
	 * 如果这个集合当中包含了A则直接把a对象从第二个map{@link DefaultSingletonBeanRegistry#singletonFactories}当中get出来并且返回
	 * 所以这一次就不等于空了，于是B就可以自动注入这个a对象了
	 * 这个时候a还只是对象，a这个对象里面依赖的B还没有注入
	 * 当b对象注入完成a之后，把B的周期走完，存到容器当中
	 *
	 * 以上就完成了在A注入属性时把B的注入完成（B注入属性时用已经创建好的A对象。
	 * 完成了A的属性注入之后，返回继续完成A的Bean生命周期，
	 *
	 * 当b创建完成一个bean之后，返回b(b已经是一个bean了)
	 * 需要说明的b是一个bean意味着b已经注入完成了a；这点上面已经说明了
	 * 由于返回了一个b，故而a也能注入b了；
	 * 接着a对象继续完成生命周期，当走完之后a也在容器中了。
	 *
	 * 至此循环依赖搞定。
	 **/
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {

		if(beanName.equals("beanA") || beanName.equals("beanB")) {
			LogUtil.printObject("FlagA [class]=" + this.getClass()
					+ "\n [beanName]=" + beanName
					+ "\n[allowEarlyReference]=" + allowEarlyReference);
		}
		/**
		 首先spring会去第一个map当中去获取一个bean：从容器中获取。
		 说明我们如果在容器初始化后调用getBean其实就从map中去获取一个bean
		 假设是初始化A的时候那么这个时候肯定等于空，前文分析过这个map的意义
		 */
		Object singletonObject = this.singletonObjects.get(beanName);

		/**
		 我们这里的场景是初始化对象A第一次调用这个方法
		 这段代码非常重要，首先从容器中拿，如果拿不到，再判断这个对象是不是在set集合
		 set集合{@link #singletonsCurrentlyInCreation} ，
		 使用 {@link #beforeSingletonCreation(String)}来加入到Set中，
		 使用 {@link #afterSingletonCreation(String)}从Set中移除——对象创建完成后，注意不是bean创建完成走。
		 具体看这里 {@link AbstractAutowireCapableBeanFactory#getSingletonFactoryBeanForTypeCheck(String, RootBeanDefinition)}
		 假设现在a不在创建过程，那么直接返回一个空，第一次getSingleton返回
		 **/
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				//从一个对象池中拿到对象
				singletonObject = this.earlySingletonObjects.get(beanName);
				if(beanName.equals("beanA") || beanName.equals("beanB")) {
					LogUtil.printObject("FlagB [class]=" + this.getClass()
							+ "\n [beanName]=" + beanName
							+ "\n[allowEarlyReference]=" + allowEarlyReference);
				}
				if (singletonObject == null && allowEarlyReference) {
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					/**
					 * 如
					 * <pre>
					 *     Class A {
					 *         B b;
					 *     }
					 *     Class B {
					 *         A a;
					 *     }
					 * </pre>
					 * 假设先注入A，那么最后下面的代码由A会调用，B不会调用。<br/>
					 * 也就是会把A先存放到{@link DefaultSingletonBeanRegistry#singletonFactories}中。<br/>
					 * 在B注入属性时从{@link DefaultSingletonBeanRegistry#singletonFactories}中拿出已经做好准备的A对象<br/>
					 */
					if (singletonFactory != null) {
						if(beanName.equals("beanA") || beanName.equals("beanB")) {
							LogUtil.printObject("FlagC [class]=" + this.getClass()
									+ "\n [beanName]=" + beanName
									+ "\n[allowEarlyReference]=" + allowEarlyReference);
						}
						//调用表达式，说白了就是调用工厂的方法，然后改变对象(代理或者包装一些特性)
						//我们假设对象不需要改变的情况那么返回了原对象就是a
						singletonObject = singletonFactory.getObject();
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
						/**
						 * 重点:为什么要放到第三个？为什么要移除第二个？
						 * 首先我们通过分析做一个总结:
						 * spring首先从第一个map中拿a这个bean
						 * 拿不到，从第三个map当中拿a这个对象
						 * 拿不到，从第二个map拿a这个对象或者工厂
						 * 拿到之后放到第三个map，移除第二个map里面的表达式、或者工厂
						 * 如果对象需要改变，当改变完成之后就把他放到第三个里面
						 * 这里的情况是b需要a而进行的步骤，试想一下以后如果还有C需要依赖a
						 * 就不需要重复第二个map的工作了，也就是改变对象的工作了。
						 * 因为改变完成之后的a对象已经在第三个map中了。
						 * 如果对象不需要改变道理是一样的，也同样在第三个map取就是了；
						 * 至于为什么需要移除第二个map里面的工厂、或者表达式就更好理解了
						 * 他已经对a做完了改变，改变之后的对象已经在第三个map了，为了方便gc啊
						 *
						 *
						 * 为什么需要改变对象？那个表达式、或者说工厂主要干什么事呢？ 那个工厂、或者表达式主要是调用了下面这个方法
						 * {@link AbstractAutowireCapableBeanFactory#getEarlyBeanReference(String, RootBeanDefinition, Object)}
						 */
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			//首先也是从第一个map即容器中获取
			//再次证明如果我们在容器初始化后调用getBean其实就是从map当中获取一个bean
			//我们这里的场景是初始化对象A第一次调用这个方法
			//那么肯定为空
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				/**
				 * 注意这行代码，就是A的名字添加到set集合当中
				 也就是笔者说的标识A正在创建过程当中
				 这个方法比较简单我就不单独分析了，直接在这里给出
				 singletonsCurrentlyInCreation.add就是放到set集合当中
				 */
				beforeSingletonCreation(beanName);

				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//这里便是创建一个bean的入口了
					//spring会首先实例化一个对象，然后走生命周期
					//走生命周期的时候前面说过会判断是否允许循环依赖
					//如果允许则会把创建出来的这个对象放到第二个map当中
					//然后接着走生命周期当他走到属性填充的时候
					//会去get一下B，因为需要填充B，也就是大家认为的自动注入
					//这些代码下文分析，如果走完了生命周期
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					/**
					 * 此事bean依然没有创建，仅仅只有一个对象存在。
					 * 与 {@link AbstractAutowireCapableBeanFactory#getSingletonFactoryBeanForTypeCheck(String, RootBeanDefinition)}
					 * 区别在于
					 * 此处从{@link #singletonFactories}中获取对象；
					 * 而{@link AbstractAutowireCapableBeanFactory#getSingletonFactoryBeanForTypeCheck(String, RootBeanDefinition)}
					 * 使用无参构造创建一个对象。也是供此处使用的对象。
					 * 因为此处是第二次调用是触发的处理。
					 */
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	@Override
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
