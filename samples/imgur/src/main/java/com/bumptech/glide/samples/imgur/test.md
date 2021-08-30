使用流程：
```
// 获取一个requestManager，每个Activity与Fragment分别对应一个RequestManager，，因为要和页面的生命周期绑定
Glide.with(vh.imageView)
  // load默认替我们执行了asDrawable方法
  // 返回一个RequestBuilder-实际上是GlideRequest，每进行一次请求，就会构建一个GlideRequest实例，这个对象是运行时生成的类
  .load(image.link)
  //
  .into(vh.imageView);
```
Glide对象，单例，全局唯一，依据applicationContext创建出来的，创建时会构造一系列的线程池：
```
sourceExecutor = GlideExecutor.newSourceExecutor();
diskCacheExecutor = GlideExecutor.newDiskCacheExecutor();
animationExecutor = GlideExecutor.newAnimationExecutor();
memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
```

with(vh.imageView)会返回一个RequestManager对象：
```
创建RequestManager对象分三种情况：
子线程时，根据applicationContext创建
主线程时，判断当前imageview的context是否是activity，如果是，则根据当前activity查找对应的那个透明的fragment，如果不
是activity，则根据view找到对应的fragment（RequestManagerRetriver保存了一个ArrayMap用于存储view与对应的frag键值对），
最后根据找到的activity或者fragment获取到对应的ResourceManager实例
    // 校验当前context是否是activity
    private static Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }

// 不管是activity还是fragment，都是调用的这个，注意传进去了一个fm-FragmentManager，就是为了构造对应的Fragment
RequestManager resourceManager = fragmentGet(activity, fm, null, isActivityVisible(activity));    
public RequestManager build(
    @NonNull Glide glide,
    @NonNull Lifecycle lifecycle,
    @NonNull RequestManagerTreeNode requestManagerTreeNode,
    @NonNull Context context) {
        return new RequestManager(glide, lifecycle, requestManagerTreeNode, context);
}    
```
>* 同一个 Activity 对应一个 FragmentManager，一个 FragmentManager 对应一个 RequestManagerFragment，一个 RequestManagerFragment 对应一个 RequestManager，所以一个 Activity 对应 一个 RequestManager；
* 同一个 Fragment 同样可得出上述结论；
* 但如果 Fragment 属于 Activity，或者 Fragment 属于 Fragment，在 Activity、Framgnent 中分别创建 Glide 请求是并不会只创建一个 RequestManager；
* 子线程发起 Glide 请求或传入对象为 ApplicationContext，则使用全局单例的 RequestManager。

ResourceManager的作用
1. 构建RequestBuilder发送请求
2. 根据关联的Fragment的生命周期来控制Request的生命周期
