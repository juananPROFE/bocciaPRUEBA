package com.example.bocciaprueba;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
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

    public Mat processingImage(){
        Mat frameHSV=new Mat();
        Mat maskRed_1=new Mat();
        Mat maskRed_2=new Mat();
        Mat maskRed=new Mat();
        Mat frameRed=new Mat();
        Mat maskFinal=new Mat();
        Mat maskBlue_2=new Mat();
        Mat maskBlue=new Mat();
        Mat frameBlue=new Mat();
        Mat frameFinal=new Mat();
        Mat hierachy=new Mat();
        MatOfPoint c=new MatOfPoint();
        MatOfInt cSuave=new MatOfInt();
        double area;
        ArrayList <MatOfPoint> contours=new ArrayList<MatOfPoint>();
        ArrayList <MatOfInt> hullPoints=new ArrayList<MatOfInt>();
        ArrayList <MatOfPoint> contours_final=new ArrayList<MatOfPoint>();
        //bolas rojas
        Imgproc.cvtColor(frame,frameHSV,Imgproc.COLOR_BGR2HSV);
        Core.inRange(frameHSV,new Scalar(0,100,20),new Scalar(175,255,255),maskRed_1);
        Core.inRange(frameHSV,new Scalar(175,100,20),new Scalar(179,255,255),maskRed_2);
        Core.inRange(frameHSV,new Scalar(90,100,20),new Scalar(130,255,255),maskBlue);
        Core.add(maskRed_1,maskRed_2,maskRed);
        Core.add(maskRed,maskBlue,maskFinal);
        Core.bitwise_and(frame,frame,frameFinal,maskFinal);

        Imgproc.findContours(maskFinal,contours,new Mat(),Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        for (int ind=0; ind<contours.size();ind++) {
            c=contours.get(ind);
            area = Imgproc.contourArea(c);
            if (area>3000){
                //Imgproc.convexHull(c,cSuave);
                //hullPoints.add(cSuave);
                contours_final.add(c);
            }
        }

        /*for(int j=0; j < hullPoints.size(); j++){
            MatOfPoint m = new MatOfPoint();
            m.fromArray(hullPoints.get(j));
            contours_final.add(m);
        }*/
        Imgproc.drawContours(frameFinal,contours_final,-1,new Scalar(255,255,255),Imgproc.LINE_4);


        return frameFinal;
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
