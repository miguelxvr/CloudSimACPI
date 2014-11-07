/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudbus.cloudsim.xml;

/**
 *
 * @author Miguel Xavier
 */
public class ACPIStateDatas {

    private double power_entering;
    private int time_entering;
    private double power_leaving;
    private int time_leaving;
    private double power_staying;
    private int time_staying;

    public double getPower_entering() {
        return power_entering;
    }

    public void setPower_entering(double power_entering) {
        this.power_entering = power_entering;
    }

    public int getTime_entering() {
        return time_entering;
    }

    public void setTime_entering(int time_entering) {
        this.time_entering = time_entering;
    }

    public double getPower_leaving() {
        return power_leaving;
    }

    public void setPower_leaving(double power_leaving) {
        this.power_leaving = power_leaving;
    }

    public int getTime_leaving() {
        return time_leaving;
    }

    public void setTime_leaving(int time_leaving) {
        this.time_leaving = time_leaving;
    }

    public double getPower_staying() {
        return power_staying;
    }

    public void setPower_staying(double power_staying) {
        this.power_staying = power_staying;
    }

    public int getTime_staying() {
        return time_staying;
    }

    public void setTime_staying(int time_staying) {
        this.time_staying = time_staying;
    }
}
