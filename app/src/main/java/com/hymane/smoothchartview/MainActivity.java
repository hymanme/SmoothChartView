package com.hymane.smoothchartview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.hymane.smoothchart.OnChartClickListener;
import com.hymane.smoothchart.SmoothLineChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SmoothLineChartView chartView = (SmoothLineChartView) findViewById(R.id.smoothChartView);
        chartView.setCustomBorder(true);
        chartView.setTagDrawable(R.mipmap.tag);
        chartView.setTextColor(Color.WHITE);
        chartView.setTextSize(10);
        chartView.setTextOffset(4);
        chartView.setMinY(40);
        chartView.setMaxY(58);
        chartView.enableShowTag(true);
        chartView.enableDrawArea(true);
        chartView.setLineColor(Color.parseColor("#FFDCDCDC"));
        chartView.setCircleColor(Color.parseColor("#FFE30252"));
        chartView.setInnerCircleColor(Color.parseColor("#ffffff"));
        chartView.setNodeStyle(SmoothLineChartView.NODE_STYLE_RING);
        List<Float> data = new ArrayList<>();
        data.add(55f);
        data.add(54f);
        data.add(51f);
        data.add(49f);
        data.add(51f);
        data.add(52f);
        data.add(51f);
        List<String> x = new ArrayList<>();
        x.add("3-12");
        x.add("3-13");
        x.add("3-14");
        x.add("3-15");
        x.add("3-16");
        x.add("3-17");
        x.add("3-18");
        chartView.setData(data, x);

        chartView.setOnChartClickListener(new OnChartClickListener() {
            @Override
            public void onClick(int position, float value) {
//                chartView.remove(position);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chartView.add(new Random().nextFloat() * 5 + 50, "3-12");
            }
        });

        ((SeekBar) findViewById(R.id.min)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < chartView.getMaxY())
                    chartView.setMinY(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((SeekBar) findViewById(R.id.max)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > chartView.getMinY())
                    chartView.setMaxY(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((CheckBox) findViewById(R.id.cb_show_area)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                chartView.enableDrawArea(isChecked);
            }
        });
        ((CheckBox) findViewById(R.id.cb_show_tag)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                chartView.enableShowTag(isChecked);
            }
        });
        ((RadioGroup) findViewById(R.id.rg_style)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.rb_rg_style_1) {
                    chartView.setNodeStyle(SmoothLineChartView.NODE_STYLE_RING);
                } else if (checkedId == R.id.rb_rg_style_2) {
                    chartView.setNodeStyle(SmoothLineChartView.NODE_STYLE_CIRCLE);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
