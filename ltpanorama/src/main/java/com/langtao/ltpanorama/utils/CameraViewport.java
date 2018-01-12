package com.langtao.ltpanorama.utils;


import static com.langtao.ltpanorama.utils.MatrixHelper.beEqualTo;

/**
 * Created by zzr on 2017/7/28.
 */

public class CameraViewport {

    private static final String TAG = "CameraViewport";

    public float cx; // 摄像机位置x
    public float cy; // 摄像机位置y
    public float cz; // 摄像机位置z
    private Geometry.Vector _eye;

    public float tx; // 摄像机目标点x
    public float ty; // 摄像机目标点y
    public float tz; // 摄像机目标点z
    private Geometry.Vector _lookAt;
    private float _lookAt_length;

    public float upx;// 摄像机UP向量X分量
    public float upy;// 摄像机UP向量Y分量
    public float upz;// 摄像机UP向量Z分量
    private Geometry.Vector _up;

    private float current_horizontal_angle=0;
    private float current_vertical_angle=0;


    public CameraViewport updateAllVector(){
        this.cx = _eye.x;
        this.cy = _eye.y;
        this.cz = _eye.z;
        this.tx = _lookAt.x;
        this.ty = _lookAt.y;
        this.tz = _lookAt.z;
        this.upx= _up.x;
        this.upy= _up.y;
        this.upz= _up.z;
        //Log.d(TAG, "current_horizontal_angle : "+current_horizontal_angle);
        //Log.d(TAG, "current_vertical_angle : "+current_vertical_angle);

        //Log.w(TAG, "current All Vector: " + "\n" +
        //        this.cx + " " +  this.cy + " " +  this.cz + "\n" +
        //        this.tx + " " +  this.ty + " " +  this.tz + "\n" +
        //        this.upx + " " + this.upy + " " + this.upz + "\n");
        return this;
    }


    private void upgrade_UpVector(float current_horizontal_angle, float current_vertical_angle) {

        float sin_horizontal = (float) Math.sin(Math.toRadians(current_horizontal_angle+180f));
        float cos_horizontal = (float) Math.cos(Math.toRadians(current_horizontal_angle+180f));

        float temp_x = _lookAt_length * sin_horizontal;
        float temp_z = _lookAt_length * cos_horizontal;

        float sin_vertical = (float) Math.sin(Math.toRadians(current_vertical_angle+90f));
        float cos_vertical = (float) Math.cos(Math.toRadians(current_vertical_angle+90f));

        temp_x = temp_x * Math.abs(cos_vertical);
        temp_z = temp_z * Math.abs(cos_vertical);

        float temp_y = _lookAt_length * sin_vertical;

        _up.x = temp_x;
        _up.y = temp_y;
        _up.z = temp_z;
    }


    public CameraViewport horizontalRotation(float angle){
        float diff = Math.abs(angle - current_horizontal_angle);
        if (_lookAt == null) {
            return null;
        }

        if(Float.compare(diff,0.01f) != 0){

            float sin = (float) Math.sin(Math.toRadians(angle));
            float cos = (float) Math.cos(Math.toRadians(angle));

            _lookAt.x = _lookAt_length * sin;
            _lookAt.z = _lookAt_length * cos;

            current_horizontal_angle = angle;
            //upgrade_UpVector(current_horizontal_angle,current_vertical_angle);
        }
        updateAllVector();
        return this;
    }

    public CameraViewport verticalRotation(float angle){
        angle += 360f;
        float diff = Math.abs(angle - current_vertical_angle);
        if (_lookAt == null) {
            return null;
        }
        //Log.d(TAG, "verticalRotation diff : "+diff);
        if(Float.compare(diff,0.01f) != 0){

            float sin = (float) Math.sin(Math.toRadians(angle));
            float cos = (float) Math.cos(Math.toRadians(angle));

            _lookAt.y = _lookAt_length * sin;
            _lookAt.x = _lookAt.x * cos;
            _lookAt.z = _lookAt.z * cos;

            current_vertical_angle = angle;
            //upgrade_UpVector(current_horizontal_angle,current_vertical_angle);
            if(angle > 90f && angle < 270f){
                _up.x =0.0f;
                _up.y =-1.0f;
                _up.z =0.0f;
            } else {
                _up.x =0.0f;
                _up.y =1.0f;
                _up.z =0.0f;
            }
        }
        updateAllVector();
        return this;
    }


    public CameraViewport setCameraVector(float cx,float cy,float cz){
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        _eye = new Geometry.Vector(cx,cy,cz);
        return this;
    }

    public CameraViewport setTargetViewVector(float tx,float ty,float tz){
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        _lookAt = new Geometry.Vector(tx,ty,tz);
        _lookAt_length = _lookAt.length();
        return this;
    }

    public CameraViewport setCameraUpVector(float upx,float upy,float upz){
        this.upx = upx;
        this.upy = upy;
        this.upz = upz;
        _up = new Geometry.Vector(upx,upy,upz);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        CameraViewport tartgetEye = (CameraViewport) o;
        if(
                beEqualTo(tartgetEye.cx , this.cx) &&
                beEqualTo(tartgetEye.cy , this.cy) &&
                beEqualTo(tartgetEye.cz , this.cz) &&
                beEqualTo(tartgetEye.tx , this.tx) &&
                beEqualTo(tartgetEye.ty , this.ty) &&
                beEqualTo(tartgetEye.tz , this.tz) &&
                beEqualTo(tartgetEye.upx , this.upx) &&
                beEqualTo(tartgetEye.upy , this.upy) &&
                beEqualTo(tartgetEye.upz , this.upz)
        ){
            return true;
        }else{
            return false;
        }
    }



    public void copyTo(CameraViewport tartgetEye) {
        if(tartgetEye != null){
            tartgetEye.cx = this.cx;
            tartgetEye.cy = this.cy;
            tartgetEye.cz = this.cz;

            tartgetEye.tx = this.tx;
            tartgetEye.ty = this.ty;
            tartgetEye.tz = this.tz;

            tartgetEye.upx = this.upx;
            tartgetEye.upy = this.upy;
            tartgetEye.upz = this.upz;
        } else {
            return;
        }
    }
}
