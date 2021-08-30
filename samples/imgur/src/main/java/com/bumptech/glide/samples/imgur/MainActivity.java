package com.bumptech.glide.samples.imgur;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.samples.imgur.api.Image;
import dagger.android.AndroidInjection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/** Displays images and GIFs from Imgur in a scrollable list of cards. */
public final class MainActivity extends AppCompatActivity {

  @Inject
  @Named("hotViralImages")
  Observable<List<Image>> fetchImagesObservable;

  private ImgurImageAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

    recyclerView.setHasFixedSize(true);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new ImgurImageAdapter();
    recyclerView.setAdapter(adapter);

    fetchImagesObservable
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Observer<List<Image>>() {
              @Override
              public void onCompleted() {}

              @Override
              public void onError(Throwable e) {}

              @Override
              public void onNext(List<Image> images) {
                List<Image> tmp = new ArrayList<>();
                tmp.add(images.get(0));
                adapter.setData(tmp);
              }
            });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    fetchImagesObservable.unsubscribeOn(AndroidSchedulers.mainThread());
  }

  private final class ImgurImageAdapter extends RecyclerView.Adapter<ViewHolder> {

    private List<Image> images = Collections.emptyList();

    public void setData(@NonNull List<Image> images) {
      this.images = images;
      notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ViewHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.image_card, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      ViewHolder vh = (ViewHolder) holder;
      Image image = images.get(position);
      vh.title.setText(TextUtils.isEmpty(image.title) ? image.description : image.title);
      // ImgurGlide.with(vh.imageView).load(image.link).into(vh.imageView);
      // TODO: 2021/8/30 这里是起始点
      /*
       * Glide对象 单例 全局唯一
       * RequestManager 单例 当前Activity/Fragment中唯一
       * */
      // 获取一个requestManager，每个Activity与Fragment分别对应一个RequestManager，，因为要和页面的生命周期绑定
      // 如果是在子线程中加载图片，则会使用application的context，获取的RequestManager是全局单例的，生命周期是整个进程的生命周期
      //
      Glide.with(vh.imageView)
          // load默认替我们执行了asDrawable方法
          // 返回一个RequestBuilder-实际上是GlideRequest，每进行一次请求，就会构建一个GlideRequest实例，这个对象是运行时生成的类
          .load(image.link)
          //
          .into(vh.imageView);
    }

    @Override
    public int getItemCount() {
      return images.size();
    }

    private final class ViewHolder extends RecyclerView.ViewHolder {

      private final ImageView imageView;
      private final TextView title;

      ViewHolder(View itemView) {
        super(itemView);
        imageView = (ImageView) itemView.findViewById(R.id.image);
        title = (TextView) itemView.findViewById(R.id.title);
      }
    }
  }
}
