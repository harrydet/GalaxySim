
/*
* "Physics" part of code adapted from Dan Schroeder's applet at:
*
*     http://physics.weber.edu/schroeder/software/mdapplet.html
*/



import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;

import java.awt.* ;
import java.awt.Color;
import java.util.Random;
import javax.swing.* ;

public class DualAttempt{

    // Size of simulation
    final static int N = 4000 ;  // Number of "stars"
    final static double BOX_WIDTH = 100;


    // Initial state

    final static double RADIUS = 20 ;  // of randomly populated sphere

    final static double ANGULAR_VELOCITY = 0.4;
    // controls total angular momentum


    // Simulation




    // Display

    final static int WINDOW_SIZE =800  ;
    final static int DELAY = 0 ;
    final static int OUTPUT_FREQ = 100 ;





    public static void main(String args []) throws Exception {

        Device gpu1 = Device.firstGPU();
        Device gpu2 = Device.best();
        System.out.println(gpu1.toString());
        System.out.println(gpu2.toString());

        // Star positions
        final double [] positionsX1 = new double [N/2] ;
        final double [] positionsY1 = new double [N/2] ;
        final double [] positionsZ1 = new double [N/2] ;
        final double [] positionsX2 = new double [N/2] ;
        final double [] positionsY2 = new double [N/2] ;
        final double [] positionsZ2 = new double [N/2] ;

        // Star velocities
        final double [] velocitiesX = new double [N] ;
        final double [] velocitiesY = new double [N] ;
        final double [] velocitiesZ = new double [N] ;

        // Star accelerations
        final double [] accelerationsX1 = new double [N/2] ;
        final double [] accelerationsY1 = new double [N/2] ;
        final double [] accelerationsZ1 = new double [N/2] ;
        final double [] accelerationsX2 = new double [N/2] ;
        final double [] accelerationsY2 = new double [N/2] ;
        final double [] accelerationsZ2 = new double [N/2] ;

        final double blackHolePositionX = BOX_WIDTH * 0.5;
        final double blackHolePositionY = BOX_WIDTH * 0.5;
        final double blackHolePositionZ = BOX_WIDTH * 0.5;

        final double BLACK_HOLE_SIZE = 0;



        final double DT = 0.002 ;  // Time step

        Display display = new Display(positionsX1, positionsY1, positionsX2, positionsY2);

        final Kernel kernel1 = new Kernel(){
            @Override public void run() {
                int gid = getGlobalId();
               // System.out.println("1: " + gid);

                accelerationsX1[gid] = 0.0;
                accelerationsY1[gid] = 0.0;
                accelerationsZ1[gid] = 0.0;

                double distanceX, distanceY, distanceZ;  // separations in positionsX and positionsY directions
                double distanceXSqr, distanceYSqr, distanceZSqr, rSquared, r, rCubedInv, fx, fy, fz;


                for (int j = 0; j < N; j++) {  // loop over all distinct pairs
                    if (gid != j && gid != j - N/2) {

                        if(j >= N/2){

                            distanceX = (positionsX1[gid] - positionsX2[j - N/2]);
                            distanceY = (positionsY1[gid] - positionsY2[j - N/2]);
                            distanceZ = (positionsZ1[gid] - positionsZ2[j - N/2]);

                            distanceXSqr = distanceX * distanceX;
                            distanceYSqr = distanceY * distanceY;
                            distanceZSqr = distanceZ * distanceZ;
                            rSquared = distanceXSqr + distanceYSqr + distanceZSqr;
                            r = Math.sqrt(rSquared);
                            rCubedInv = 1.0 / (rSquared * r);
                            fx = -rCubedInv * distanceX;
                            fy = -rCubedInv * distanceY;
                            fz = -rCubedInv * distanceZ;

                            accelerationsX1[gid] += fx;  // add this force on to i's acceleration (mass = 1)
                            accelerationsY1[gid] += fy;
                            accelerationsZ1[gid] += fz;
                        } else {
                            distanceX = (positionsX1[gid] - positionsX1[j]);
                            distanceY = (positionsY1[gid] - positionsY1[j]);
                            distanceZ = (positionsZ1[gid] - positionsZ1[j]);

                            distanceXSqr = distanceX * distanceX;
                            distanceYSqr = distanceY * distanceY;
                            distanceZSqr = distanceZ * distanceZ;
                            rSquared = distanceXSqr + distanceYSqr + distanceZSqr;
                            r = Math.sqrt(rSquared);
                            rCubedInv = 1.0 / (rSquared * r);
                            fx = -rCubedInv * distanceX;
                            fy = -rCubedInv * distanceY;
                            fz = -rCubedInv * distanceZ;

                            accelerationsX1[gid] += fx;  // add this force on to i's acceleration (mass = 1)
                            accelerationsY1[gid] += fy;
                            accelerationsZ1[gid] += fz;
                        }

                        // Vector version of inverse square law

//                accelerationsX[j] -= fx;  // Newton's 3rd law
//                accelerationsY[j] -= fy;
//                accelerationsZ[j] -= fz;
                    }
                }



            }
        };

        final Kernel kernel2 = new Kernel(){

            @Override public void run() {
                int gid = getGlobalId();
                //System.out.println("2: " + gid);

                accelerationsX2[gid] = 0.0;
                accelerationsY2[gid] = 0.0;
                accelerationsZ2[gid] = 0.0;

                double distanceX2, distanceY2, distanceZ2;  // separations in positionsX and positionsY directions
                double distanceXSqr2, distanceYSqr2, distanceZSqr2, rSquared2, r2, rCubedInv2, fx2, fy2, fz2;
                for (int j = 0; j < N; j++) {  // loop over all distinct pairs
                    if (gid != j && gid != j - N/2) {
                        // Vector version of inverse square law
                        if(j >= N/2){
                            distanceX2 = (positionsX2[gid] - positionsX2[j - N/2]);
                            distanceY2 = (positionsY2[gid] - positionsY2[j - N/2]);
                            distanceZ2 = (positionsZ2[gid] - positionsZ2[j - N/2]);
                            distanceXSqr2 = distanceX2 * distanceX2;
                            distanceYSqr2 = distanceY2 * distanceY2;
                            distanceZSqr2 = distanceZ2 * distanceZ2;
                            rSquared2 = distanceXSqr2 + distanceYSqr2 + distanceZSqr2;
                            r2 = Math.sqrt(rSquared2);
                            rCubedInv2 = 1.0 / (rSquared2 * r2);
                            fx2 = -rCubedInv2 * distanceX2;
                            fy2 = -rCubedInv2 * distanceY2;
                            fz2 = -rCubedInv2 * distanceZ2;

                            accelerationsX2[gid] += fx2;  // add this force on to i's acceleration (mass = 1)
                            accelerationsY2[gid] += fy2;
                            accelerationsZ2[gid] += fz2;
                        } else {
                            distanceX2 = (positionsX2[gid] - positionsX1[j]);
                            distanceY2 = (positionsY2[gid] - positionsY1[j]);
                            distanceZ2 = (positionsZ2[gid] - positionsZ1[j]);
                            distanceXSqr2 = distanceX2 * distanceX2;
                            distanceYSqr2 = distanceY2 * distanceY2;
                            distanceZSqr2 = distanceZ2 * distanceZ2;
                            rSquared2 = distanceXSqr2 + distanceYSqr2 + distanceZSqr2;
                            r2 = Math.sqrt(rSquared2);
                            rCubedInv2 = 1.0 / (rSquared2 * r2);
                            fx2 = -rCubedInv2 * distanceX2;
                            fy2 = -rCubedInv2 * distanceY2;
                            fz2 = -rCubedInv2 * distanceZ2;

                            accelerationsX2[gid] += fx2;  // add this force on to i's acceleration (mass = 1)
                            accelerationsY2[gid] += fy2;
                            accelerationsZ2[gid] += fz2;
                        }

//                accelerationsX[j] -= fx;  // Newton's 3rd law
//                accelerationsY[j] -= fy;
//                accelerationsZ[j] -= fz;
                    }
                }


            }
        };

        /*double nx = 2 * Math.random() - 1 ;
        double ny = 2 * Math.random() - 1 ;
        double nz = 2 * Math.random() - 1 ;
        double norm = 1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz) ;
        nx *= norm ;
        ny *= norm ;
        nz *= norm ;*/

        // ... or just rotate in positionsX, positionsY plane
        double nx = 0, ny = 0, nz = 1.0 ;

        // ... or just rotate in positionsX, positionsZ plane
        //double nx = 0, ny = 1.0, nz = 0 ;

        for(int i = 0 ; i < N ; i++) {

            // Place star randomly in sphere of specified radius
            double relativePosX, relativePosY, relativePosZ, radiusCheck;
            do {
                relativePosX = (2 * Math.random() - 1) * RADIUS;
                relativePosY = (2 * Math.random() - 1) * RADIUS;
                relativePosZ = (2 * Math.random() - 1) * RADIUS;
                radiusCheck = Math.sqrt(relativePosX * relativePosX + relativePosY * relativePosY + relativePosZ * relativePosZ);
            } while (radiusCheck > RADIUS);

            if(i >= N/2){
                positionsX2[i - N/2] = 0.5 * BOX_WIDTH + relativePosX;
                positionsY2[i - N/2] = 0.5 * BOX_WIDTH + relativePosY;
                positionsZ2[i - N/2] = 0.5 * BOX_WIDTH + relativePosZ;
            } else{
                positionsX1[i] = 0.5 * BOX_WIDTH + relativePosX;
                positionsY1[i] = 0.5 * BOX_WIDTH + relativePosY;
                positionsZ1[i] = 0.5 * BOX_WIDTH + relativePosZ;
            }


            velocitiesX[i] = ANGULAR_VELOCITY * (ny * relativePosZ - nz * relativePosY);
            velocitiesY[i] = ANGULAR_VELOCITY * (nz * relativePosX - nx * relativePosZ);
            velocitiesZ[i] = ANGULAR_VELOCITY * (nx * relativePosY - ny * relativePosX);

        }

        long startTime = System.currentTimeMillis();
        int iter = 0;

        final  Range range1 = gpu1.createRange(N/2);
        final Range range2 = gpu2.createRange(N/2);
        boolean alt = true;

        Thread t1;
        Thread t2;

        while(iter < 1000){
            if(iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                //System.out.println("Black hole mass: " + BLACK_HOLE_MASS[0]) ;
                display.setPositions(positionsX1, positionsY1, positionsX2, positionsY2);
                display.repaint() ;

            }

            double dtOver2 = 0.5 * DT;
            double dtSquaredOver2 = 0.5 * DT * DT;
            for (int i = 0; i < N/2; i++) {
                // update position
                positionsX1[i] += (velocitiesX[i] * DT) + (accelerationsX1[i] * dtSquaredOver2);
                positionsY1[i] += (velocitiesY[i] * DT) + (accelerationsY1[i] * dtSquaredOver2);
                positionsZ1[i] += (velocitiesZ[i] * DT) + (accelerationsZ1[i] * dtSquaredOver2);
                // update velocity halfway
                velocitiesX[i] += (accelerationsX1[i] * dtOver2);
                velocitiesY[i] += (accelerationsY1[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ1[i] * dtOver2);
            }

            for (int i = 0; i < N/2; i++) {
                // update position
                positionsX2[i] += (velocitiesX[i + N/2] * DT) + (accelerationsX2[i] * dtSquaredOver2);
                positionsY2[i] += (velocitiesY[i + N/2] * DT) + (accelerationsY2[i] * dtSquaredOver2);
                positionsZ2[i] += (velocitiesZ[i + N/2] * DT) + (accelerationsZ2[i] * dtSquaredOver2);
                // update velocity halfway
                velocitiesX[i + N/2] += (accelerationsX2[i] * dtOver2);
                velocitiesY[i + N/2] += (accelerationsY2[i] * dtOver2);
                velocitiesZ[i + N/2] += (accelerationsZ2[i] * dtOver2);
            }


            t1 = new Thread(new Runnable() {
                public void run() {
                    kernel1.execute(range1);
                }
            });
            t2 = new Thread(new Runnable() {
                public void run() {
                    kernel2.execute(range2);
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            for (int i = 0; i < N/2; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i] += (accelerationsX1[i] * dtOver2);
                velocitiesY[i] += (accelerationsY1[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ1[i] * dtOver2);
            }

            for (int i = 0; i < N/2; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i + N/2] += (accelerationsX2[i] * dtOver2);
                velocitiesY[i + N/2] += (accelerationsY2[i] * dtOver2);
                velocitiesZ[i + N/2] += (accelerationsZ2[i] * dtOver2);
            }

            iter++;
            alt = !alt;


        }
        kernel1.dispose();
        kernel2.dispose();

        System.out.println("Calculation completed in "
                + (System.currentTimeMillis() - startTime) + " milliseconds");
    }



    // Interaction forces (gravity)
    // This is where the program spends most of its time.

    // (NOTE: use of Newton's 3rd law below to essentially half number
    // of calculations needs some care in a parallel version.
    // A naive decomposition on the i loop can lead to a race condition
    // because you are assigning to accelerationsX[j], etc.
    // You can remove these assignments and extend the j loop to a fixed
    // upper bound of N, or, for extra credit, find a cleverer solution!)




    static class Display extends JPanel {

        static final double SCALEX = WINDOW_SIZE / BOX_WIDTH ;
        double [] positionsX1, positionsY1, positionsX2, positionsY2;

        Display(double [] positionsX1, double [] positionsY1, double [] positionsX2, double [] positionsY2) {

            this.positionsX1 = positionsX1;
            this.positionsY1 = positionsY1;
            this.positionsX2 = positionsX2;
            this.positionsY2 = positionsY2;
            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE)) ;


            JFrame frame = new JFrame("MD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void setPositions(double [] positionsX1, double [] positionsY1, double [] positionsX2, double [] positionsY2){
            this.positionsX1 = positionsX1;
            this.positionsY1 = positionsY1;
            this.positionsX2 = positionsX2;
            this.positionsY2 = positionsY2;
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            g.setColor(Color.WHITE) ;
            for(int i = 0 ; i < N ; i++) {
                int gx, gy;
                if(i >= N/2){
                    gx = (int) (SCALEX * this.positionsX2[i - N/2]) ;
                    gy = (int) (SCALEX * this.positionsY2[i - N/2]) ;
                } else {
                    gx = (int) (SCALEX * this.positionsX1[i]) ;
                    gy = (int) (SCALEX * this.positionsY1[i]) ;
                }
                if(0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) {
                    if(i > N/2){
                        g.setColor(Color.WHITE);
                    } else {
                        g.setColor(Color.WHITE);
                    }
                    g.fillRect(gx, gy, 1, 1) ;
                }
            }
        }
    }
}

