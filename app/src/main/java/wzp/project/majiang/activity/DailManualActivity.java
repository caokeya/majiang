package wzp.project.majiang.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import wzp.project.majiang.R;

/**
 * Created by wzp on 2017/12/10.
 */

public class DailManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dail_manual);
    }

    public static void myStartActivity(Context context) {
        Intent intent = new Intent(context, DailManualActivity.class);
        context.startActivity(intent);
    }
}