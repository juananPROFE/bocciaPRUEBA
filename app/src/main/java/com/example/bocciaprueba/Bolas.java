package com.example.bocciaprueba;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Bolas {
    private int n_rojas;
    private int n_azules;

    private Mat frame;
    Bolas(int n_rojas,int n_azules, Mat fr){
        this.n_rojas=n_rojas;
        this.n_azules=n_azules;
        this.frame=fr;
    }


    public int getN_rojas()
    {
        return this.n_rojas;
    }

    public int getN_azules()
    {
        return this.n_azules;
    }

    public Mat processingImage() {
        Mat frameFinal=new Mat();
        Mat frameWhiteRed=new Mat();
        Core.add(this.whiteBall(),this.redObjects(),frameWhiteRed);
        Core.add(frameWhiteRed,this.blueObjects(),frameFinal);
        return frameFinal;
    }

    public Mat whiteBall()
    {

        Mat frameHSV = new Mat();
        Mat frameBinary=new Mat();
        Mat maskWhite = new Mat();
        Mat frameFinal = new Mat();
        MatOfPoint c = new MatOfPoint();
        MatOfInt cSuave = new MatOfInt();
        double area;
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<MatOfInt> hullPoints = new ArrayList<MatOfInt>();
        ArrayList<MatOfPoint> contours_final = new ArrayList<MatOfPoint>();

        Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_RGB2HSV);

        Core.inRange(frameHSV, new Scalar(0, 0, 168), new Scalar(172, 20, 255), maskWhite);

        Core.bitwise_and(frame, frame, frameFinal, maskWhite);
        Imgproc.GaussianBlur(frameFinal, frameFinal, new Size(3, 3), 0);
        Imgproc.findContours(maskWhite, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int ind = 0; ind < contours.size(); ind++) {
            c = contours.get(ind);
            area = Imgproc.contourArea(c);
            if (area > 500) {
                Imgproc.convexHull(c, cSuave);
                hullPoints.add(cSuave);
                contours_final.add(convertIndexesToPoints(c, cSuave));
                Imgproc.moments(c, true);
                contours_final.add(c);
            }
        }
        Imgproc.drawContours(frameFinal, contours_final, -1, new Scalar(0, 255, 0), 2);

        return frameFinal;
    }

    public Mat redObjects()
    {
        Mat frameHSV = new Mat();
        Mat maskRed_1 = new Mat();
        Mat maskRed_2 = new Mat();
        Mat maskRed = new Mat();
        Mat maskFinal = new Mat();
        Mat frameFinal = new Mat();
        MatOfPoint c = new MatOfPoint();
        MatOfInt cSuave = new MatOfInt();
        double area;

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<MatOfInt> hullPoints = new ArrayList<MatOfInt>();
        ArrayList<MatOfPoint> contours_final = new ArrayList<MatOfPoint>();
        //bolas rojas
        Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_RGB2HSV);

        //máscara roja
        Core.inRange(frameHSV, new Scalar(0, 70, 20), new Scalar(8, 255, 255), maskRed_1);
        Core.inRange(frameHSV, new Scalar(165, 70, 20), new Scalar(179, 255, 255), maskRed_2);
        Core.add(maskRed_1, maskRed_2, maskRed);
         Core.bitwise_and(frame, frame, frameFinal,maskRed);

        Imgproc.GaussianBlur(frameFinal, frameFinal, new Size(3, 3), 0);
        Imgproc.findContours(maskRed, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int ind = 0; ind < contours.size(); ind++) {
            c = contours.get(ind);
            area = Imgproc.contourArea(c);
            if (area > 500) {
                Imgproc.convexHull(c, cSuave);
                hullPoints.add(cSuave);
                contours_final.add(convertIndexesToPoints(c, cSuave));

                Imgproc.moments(c, true);
                contours_final.add(c);
            }
        }
        this.n_rojas=contours_final.size()/2;
        Imgproc.drawContours(frameFinal, contours_final, -1, new Scalar(255, 255, 255), 2);


        return frameFinal;
    }

    public Mat blueObjects()
    {

        Mat frameHSV = new Mat();
        Mat maskFinal = new Mat();
        Mat maskBlue_2 = new Mat();
        Mat maskBlue = new Mat();
        Mat frameBlue = new Mat();
        Mat frameFinal = new Mat();
        Mat hierachy = new Mat();
        MatOfPoint c = new MatOfPoint();
        MatOfInt cSuave = new MatOfInt();
        double area;
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<MatOfInt> hullPoints = new ArrayList<MatOfInt>();
        ArrayList<MatOfPoint> contours_final = new ArrayList<MatOfPoint>();

        Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_RGB2HSV);

        //máscara azul

        Core.inRange(frameHSV, new Scalar(80, 70, 20), new Scalar(140, 255, 255), maskBlue);

        Core.bitwise_or(frame, frame, frameFinal, maskBlue);
        Imgproc.GaussianBlur(frameFinal, frameFinal, new Size(3, 3), 0);
        Imgproc.findContours(maskBlue, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int ind = 0; ind < contours.size(); ind++) {
            c = contours.get(ind);
            area = Imgproc.contourArea(c);
            if (area > 500) {
                Imgproc.convexHull(c, cSuave);
                hullPoints.add(cSuave);
                contours_final.add(convertIndexesToPoints(c, cSuave));
                Imgproc.moments(c, true);
                contours_final.add(c);
            }
        }
        this.n_azules=contours_final.size()/2;
        Imgproc.drawContours(frameFinal, contours_final, -1, new Scalar(255, 255, 255), 2);

        return frameFinal;
    }


    public static MatOfPoint convertIndexesToPoints(MatOfPoint contour, MatOfInt indexes) {
        int[] arrIndex = indexes.toArray();
        Point[] arrContour = contour.toArray();
        Point[] arrPoints = new Point[arrIndex.length];

        for (int i=0;i<arrIndex.length;i++) {
            arrPoints[i] = arrContour[arrIndex[i]];
        }

        MatOfPoint hull = new MatOfPoint();
        hull.fromArray(arrPoints);
        return hull;
    }

    public boolean sumaRoja(){
        if(this.n_rojas>=0) {
            this.n_rojas++;
            return true;
        }
        return false;
    }

    public boolean sumaAzul(){
        if(this.n_azules>=0) {
            this.n_azules++;
            return true;
        }
        return false;
    }

    public void muestraDatos()
    {
        System.out.println("Bolas rojas: "+this.n_rojas+" Bolas azules: "+this.n_azules);
    }
}
