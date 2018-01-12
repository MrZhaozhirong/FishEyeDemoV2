package com.langtao.ltpanorama.utils;

import android.graphics.Point;

/**
 * Created by zzr on 2017/9/19.
 */

public class Geometry {

    /**
     * 平面
     */
    public static class Plane{
        public final Point point;
        /* 法向量 */
        public final Vector normalVertor;
        public Plane(Point point, Vector normalVertor){
            this.point = point;
            this.normalVertor = normalVertor;
        }
    }

    /**
     * 球体
     */
    public static class Sphere{
        public final Point center;
        public final float radius;
        public Sphere(Point center, float radius){
            this.center = center;
            this.radius = radius;
        }
    }

    /**
     * 几何射线
     */
    public static class Ray{
        public final Point point;
        public final Vector vector;
        public Ray(Point point, Vector vector){
            this.point = point;
            this.vector = vector;
        }
    }



    /**
     * 有向 向量
     */
    public static class Vector{
        public float x,y,z;
        public Vector(float x,float y,float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * 向量A*向量B 交叉乘积
         * i, j, k, 分别代表是xyz轴方向的分量
         * |A*B| = (Ay*Bz-Az*By)i + (Az*Bx-Ax*Bz)j + (Ax*By-Ay*Bx)k
         * @param other vector
         */
        public Vector crossProduct(Vector other) {
            return new Vector(
                    (y * other.z) - (z * other.y),
                    (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }

        public float length() {
            double sqrt = Math.sqrt(x * x + y * y + z * z);
            return (float) sqrt;
        }

        public float dotProduct(Vector other) {
            return x*other.x + y*other.y + z*other.z;
        }

        public Vector scale(float scale) {
            return new Vector(x*scale,y*scale,z*scale);
        }

        /**
         * 交叉乘积之后的平面 的 再求法向量
         * @return
         */
        public Vector normalize() {
            return scale( 1f/length() );
        }

        public Vector increase(Vector other){
            return new Vector(this.x+other.x, this.y+other.y, this.z+other.z);
        }
        public Vector descrease(Vector other){
            return new Vector(this.x-other.x, this.y-other.y, this.z-other.z);
        }

    }

}
