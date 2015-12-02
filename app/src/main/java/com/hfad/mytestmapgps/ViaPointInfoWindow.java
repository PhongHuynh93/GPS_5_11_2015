package com.hfad.mytestmapgps;


import android.view.View;
import android.widget.Button;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.views.MapView;

/**
 * Created by huynhducthanhphong on 12/1/15.
 */

/**
 * InfoWindow: là cửa sổ khi ta click vào 1 marker nào đó, cửa số này hiện ra và hiển thị thông tin chỉ đường 
 */
public class ViaPointInfoWindow extends MarkerInfoWindow{
    private int mSelectedPoint;

    public ViaPointInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
        Button btnDelete = (Button) (mView.findViewById(R.id.bubble_delete));
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // khi ta click nút đỏ trong bảng hướng dẫn thì bảng đó sẽ mất
                MapsActivity mapsActivity = (MapsActivity)v.getContext();
                //mapsActivity.removePoint(mSelectedPoint);
                close();
            }
        });

    }

    /**
     * Hàm này sẽ gán mSelectedPoint là cái bảng thông báo (đích hay nguồn) khi ta click vào marker thì bảng này sẽ mở (onPen)
     * @param item là marker mà ta click vô 
     */
    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker)item;
        mSelectedPoint = (Integer)marker.getRelatedObject();
        super.onOpen(item);
    }
}
