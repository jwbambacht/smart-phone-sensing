package com.example.whereami;

import java.util.Arrays;

public class Bayes {

    double[] prior;
    double[] posterior;

    public Bayes() {
        this.prior = new double[]{0.125,0.125,0.125,0.125,0.125,0.125,0.125,0.125};
        this.posterior = new double[8];
    }

    // Method that calculated the posterior on base of the new probabilities and the prior
    public void calculatePosterior(double[] probabilities) {
        double norm_sum = 0;

        System.out.println(Arrays.toString(this.posterior));

        for(int j = 0; j < 8; j++) {
            double prob = this.prior[j]*probabilities[j];

            // To keep cell probabilities 'alive' we give every cell a minimum value
            if(prob < 1e-50 || Double.isNaN(prob)) {
                prob = 1e-50;
            }
            this.posterior[j] = prob;
            norm_sum += prob;
        }

        System.out.println(Arrays.toString(this.posterior));

        for(int j = 0; j < 8; j++) {
            this.posterior[j] = this.posterior[j]/norm_sum;
        }

        System.out.println(Arrays.toString(this.posterior));
        this.prior = this.posterior;
    }

    public double[] getPrior() {
        return this.prior;
    }

    public double[] getPosterior() {
        return this.posterior;
    }

    // Method that determines if a probability is bigger than a threshold
    public boolean isConverged() {
        for(double prob : this.posterior) {
            if(prob >= 0.95) {
                return true;
            }
        }
        return false;
    }
}
