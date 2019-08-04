package com.childcity.gaussianinterpolation;

class IntrpltAlgorithm {
    static final int LAGRANGE = 8;
    static final int GAUSSIAN_NORMAL = 1;
    static final int GAUSSIAN_PARAMETRIC = 2;
    static final int GAUSSIAN_SUMMARY = 4;

    private int value;

    IntrpltAlgorithm(int value){
        this.value = value & 0xFFFF;
    }

    void changeFlag(int algorithm, boolean isEnabled){
        if(isEnabled) set(algorithm);
        else unSet(algorithm);
    }

    boolean test(int algorithm){
        return algorithm == (algorithm & value);
    }

    int toInt(){
        return value;
    }

    @Override
    public String toString(){
        return "" + toInt();
    }


    private void set(int other){
        value |= other;
    }

    private void unSet(int other){
        value &= ~other;
    }
}
