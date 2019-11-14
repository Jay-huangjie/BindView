package com.jay.bindview;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jay.bindview_annotations.BindView;
import com.jay.bindviewlib.BindViewHelper;
import com.jay.bindviewlib.Unbinder;

/**
 * 自动生成的代码位于build>generated>source>apt目录下
 */
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.tv1)
    public TextView textView1;
    @BindView(R.id.tv2)
    public TextView textView2;

    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = BindViewHelper.bind(this);
        //试着运行一下吧
        textView1.setText("这是一个赋值测试");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
