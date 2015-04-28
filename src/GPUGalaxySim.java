
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

public class GPUGalaxySim{

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
    final static int OUTPUT_FREQ = 1 ;





    public static void main(String args []) throws Exception {

        Device gpu1 = Device.firstGPU();
        Device gpu2 = Device.best();
        System.out.println(gpu1.toString());
        System.out.println(gpu2.toString());

        // Star positions
        final double [] positionsX = new double [N] ;
        final double [] positionsY = new double [N] ;
        final double [] positionsZ = new double [N] ;

        // Star velocities
        final double [] velocitiesX = new double [N] ;
        final double [] velocitiesY = new double [N] ;
        final double [] velocitiesZ = new double [N] ;

        // Star accelerations
        final double [] accelerationsX = new double [N] ;
        final double [] accelerationsY = new double [N] ;
        final double [] accelerationsZ = new double [N] ;

        final double blackHolePositionX = BOX_WIDTH * 0.5;
        final double blackHolePositionY = BOX_WIDTH * 0.5;
        final double blackHolePositionZ = BOX_WIDTH * 0.5;

        final double BLACK_HOLE_SIZE = 0;



        final double DT = 0.002 ;  // Time step

        Display display = new Display(positionsX, positionsY);

        final Kernel kernel1 = new Kernel(){
            @Override public void run() {
                int gid = getGlobalId();

                accelerationsX[gid] = 0.0;
                accelerationsY[gid] = 0.0;
                accelerationsZ[gid] = 0.0;
                double distanceX, distanceY, distanceZ;  // separations in positionsX and positionsY directions
                double distanceXSqr, distanceYSqr, distanceZSqr, rSquared, r, rCubedInv, fx, fy, fz;

                for (int j = 0; j < N; j++) {  // loop over all distinct pairs
                    if (gid != j) {
                        // Vector version of inverse square law
                        distanceX = (positionsX[gid] - positionsX[j]);
                        distanceY = (positionsY[gid] - positionsY[j]);
                        distanceZ = (positionsZ[gid] - positionsZ[j]);
                        distanceXSqr = distanceX * distanceX;
                        distanceYSqr = distanceY * distanceY;
                        distanceZSqr = distanceZ * distanceZ;
                        rSquared = distanceXSqr + distanceYSqr + distanceZSqr;
                        r = Math.sqrt(rSquared);
                        rCubedInv = 1.0 / (rSquared * r);
                        fx = -rCubedInv * distanceX;
                        fy = -rCubedInv * distanceY;
                        fz = -rCubedInv * distanceZ;

                        accelerationsX[gid] += fx;  // add this force on to i's acceleration (mass = 1)
                        accelerationsY[gid] += fy;
                        accelerationsZ[gid] += fz;
//                accelerationsX[j] -= fx;  // Newton's 3rd law
//                accelerationsY[j] -= fy;
//                accelerationsZ[j] -= fz;
                    }
                }



            }
        };

        final Kernel kernel2 = new Kernel(){
            @Override public void run() {
                int gid = getGlobalId() + N/2;

                accelerationsX[gid] = 0.0;
                accelerationsY[gid] = 0.0;
                accelerationsZ[gid] = 0.0;
                double distanceX2, distanceY2, distanceZ2;  // separations in positionsX and positionsY directions
                double distanceXSqr2, distanceYSqr2, distanceZSqr2, rSquared2, r2, rCubedInv2, fx2, fy2, fz2;

                for (int j = 0; j < N; j++) {  // loop over all distinct pairs
                    if (gid != j) {
                        // Vector version of inverse square law
                        distanceX2 = (positionsX[gid] - positionsX[j]);
                        distanceY2 = (positionsY[gid] - positionsY[j]);
                        distanceZ2 = (positionsZ[gid] - positionsZ[j]);
                        distanceXSqr2 = distanceX2 * distanceX2;
                        distanceYSqr2 = distanceY2 * distanceY2;
                        distanceZSqr2 = distanceZ2 * distanceZ2;
                        rSquared2 = distanceXSqr2 + distanceYSqr2 + distanceZSqr2;
                        r2 = Math.sqrt(rSquared2);
                        rCubedInv2 = 1.0 / (rSquared2 * r2);
                        fx2 = -rCubedInv2 * distanceX2;
                        fy2 = -rCubedInv2 * distanceY2;
                        fz2 = -rCubedInv2 * distanceZ2;

                        accelerationsX[gid] += fx2;  // add this force on to i's acceleration (mass = 1)
                        accelerationsY[gid] += fy2;
                        accelerationsZ[gid] += fz2;
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


            positionsX[i] = 0.5 * BOX_WIDTH + relativePosX;
            positionsY[i] = 0.5 * BOX_WIDTH + relativePosY;
            positionsZ[i] = 0.5 * BOX_WIDTH + relativePosZ;

            velocitiesX[i] = ANGULAR_VELOCITY * (ny * relativePosZ - nz * relativePosY);
            velocitiesY[i] = ANGULAR_VELOCITY * (nz * relativePosX - nx * relativePosZ);
            velocitiesZ[i] = ANGULAR_VELOCITY * (nx * relativePosY - ny * relativePosX);

        }

        long startTime = System.currentTimeMillis();
        int iter = 0;

        final  Range range1 = gpu1.createRange(N/2);
        final Range range2 = gpu2.createRange(N/2);
        boolean alt = true;


        while(iter < 1000){
            if(iter % OUTPUT_FREQ == 0) {
                //System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                //System.out.println("Black hole mass: " + BLACK_HOLE_MASS[0]) ;
                display.setPositions(positionsX, positionsY);
                display.repaint() ;

            }

            double dtOver2 = 0.5 * DT;
            double dtSquaredOver2 = 0.5 * DT * DT;
            for (int i = 0; i < N; i++) {
                // update position
                positionsX[i] += (velocitiesX[i] * DT) + (accelerationsX[i] * dtSquaredOver2);
                positionsY[i] += (velocitiesY[i] * DT) + (accelerationsY[i] * dtSquaredOver2);
                positionsZ[i] += (velocitiesZ[i] * DT) + (accelerationsZ[i] * dtSquaredOver2);
                // update velocity halfway
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }

            kernel1.execute(range1);
            kernel2.execute(range2);

            for (int i = 0; i < N; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }

            iter++;
            alt = !alt;


        }

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
        double [] positionsX, positionsY;

        Display(double [] positionsX, double [] positionsY) {

            this.positionsX = positionsX;
            this.positionsY = positionsY;
            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE)) ;


            JFrame frame = new JFrame("MD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void setPositions(double [] positionsX, double [] positionsY){
            this.positionsX = positionsX;
            this.positionsY = positionsY;
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            g.setColor(Color.WHITE) ;
            for(int i = 0 ; i < N ; i++) {
                int gx = (int) (SCALEX * this.positionsX[i]) ;
                int gy = (int) (SCALEX * this.positionsY[i]) ;
                if(0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) {
                    if(i > N/2){
                        g.setColor(Color.RED);
                    } else {
                        g.setColor(Color.YELLOW);
                    }
                    g.fillRect(gx, gy, 1, 1) ;
                }
            }
        }
    }
}

