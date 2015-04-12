
/*
 * "Physics" part of code adapted from Dan Schroeder's applet at:
 *
 *     http://physics.weber.edu/schroeder/software/mdapplet.html
 */



import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;
import sun.applet.AppletViewerFactory;

import java.awt.* ;
import java.awt.Color;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import javax.swing.* ;

public class GPUGalaxySim extends Thread{

    // Size of simulation
    final static int N = 2000 ;  // Number of "stars"
    final static double BOX_WIDTH = 50.0 ;


    // Initial state

    final static double RADIUS = 55;  // of randomly populated sphere

    final static double ANGULAR_VELOCITY = 1;
    // controls total angular momentum


    // Simulation




    // Display

    final static int WINDOW_SIZE =800 ;
    final static int DELAY = 0 ;
    final static int OUTPUT_FREQ = 1 ;





    public static void main(String args []) throws Exception {

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

        // Star masses
        final double [] masses = new double [N];

        final double DT = 0.002 ;  // Time step
        final double G = 6.667 * Math.pow(10, -11);
        final double AVG_DISTANCE = 3.784*Math.pow(10, 10);

        Display display = new Display(positionsX, positionsY);

        Kernel kernel = new Kernel(){
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

                distanceX = (positionsX[gid] - blackHolePositionX);
                distanceY = (positionsY[gid] - blackHolePositionY);
                distanceZ = (positionsZ[gid] - blackHolePositionZ);

                distanceXSqr = distanceX * distanceX;
                distanceYSqr = distanceY * distanceY;
                distanceZSqr = distanceZ * distanceZ;

                rSquared = distanceXSqr + distanceYSqr + distanceZSqr;
                r = Math.sqrt(rSquared);
                rCubedInv = 1.0 / (rSquared * r);
                fx = -rCubedInv * distanceX * Math.pow(10, 5);
                fy = -rCubedInv * distanceY * Math.pow(10, 5);
                fz = -rCubedInv * distanceZ * Math.pow(10, 5);

                accelerationsX[gid] += fx;  // add this force on to i's acceleration (mass of black hole is equal to 10 ^ 6 * massOfStar)
                accelerationsY[gid] += fy;
                accelerationsZ[gid] += fz;

            }
        };

        double nx = 2 * Math.random() - 1 ;
        double ny = 2 * Math.random() - 1 ;
        double nz = 2 * Math.random() - 1 ;
        double norm = 1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz) ;
        nx *= norm ;
        ny *= norm ;
        nz *= norm ;

        // ... or just rotate in positionsX, positionsY plane
        //double nx = 0, ny = 0, nz = 1.0 ;

        // ... or just rotate in positionsX, positionsZ plane
        //double nx = 0, ny = 1.0, nz = 0 ;

        for(int i = 0 ; i < N ; i++) {

            // Place star randomly in sphere of specified radius
            double relativePosX, relativePosY, relativePosZ, radiusCheck ;
            do {
                relativePosX = (2 * Math.random() - 1) * RADIUS ;
                relativePosY = (2 * Math.random() - 1) * RADIUS ;
                relativePosZ = (2 * Math.random() - 1) * RADIUS ;
                radiusCheck = Math.sqrt(relativePosX * relativePosX + relativePosY * relativePosY + relativePosZ * relativePosZ) ;
            } while(radiusCheck > RADIUS) ;

            positionsX[i] = 0.5 * BOX_WIDTH + relativePosX ;
            positionsY[i] = 0.5 * BOX_WIDTH + relativePosY ;
            positionsZ[i] = 0.5 * BOX_WIDTH + relativePosZ ;

            velocitiesX[i] = ANGULAR_VELOCITY * (ny * relativePosZ - nz * relativePosY) ;
            velocitiesY[i] = ANGULAR_VELOCITY * (nz * relativePosX - nx * relativePosZ) ;
            velocitiesZ[i] = ANGULAR_VELOCITY * (nx * relativePosY - ny * relativePosX) ;
        }

        long startTime = System.currentTimeMillis();
        int iter = 0;
        while(iter < 1000000000){
            if(iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                display.setPositions(positionsX, positionsY);
                display.repaint() ;
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
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


            kernel.execute(Range.create(N));




            for (int i = 0; i < N; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }

            iter++;
        }
        kernel.dispose();

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

        static final double SCALE = WINDOW_SIZE / BOX_WIDTH ;
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
                int gx = (int) (SCALE * this.positionsX[i]) ;
                int gy = (int) (SCALE * this.positionsY[i]) ;
                if(0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) {
                    g.fillRect(gx, gy, 1, 1) ;
                }
            }
            int gx = (int) (  WINDOW_SIZE * 0.5);
            int gy = (int) ( WINDOW_SIZE * 0.5);
            g.setColor(Color.RED);
            g.fillRect(gx, gy, 1, 1) ;
        }
    }
}

