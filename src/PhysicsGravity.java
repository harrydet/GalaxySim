
/*
 * "Physics" part of code adapted from Dan Schroeder's applet at:
 *
 *     http://physics.weber.edu/schroeder/software/mdapplet.html
 */



import sun.applet.AppletViewerFactory;

import java.awt.* ;
import java.awt.Color;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import javax.swing.* ;

public class PhysicsGravity extends Thread{

    // Size of simulation
    final static int P = 8;
    final static int N = 4000 ;  // Number of "stars"
    final static double BOX_WIDTH = 100.0 ;


    // Initial state

    final static double RADIUS = 50 ;  // of randomly populated sphere

    final static double ANGULAR_VELOCITY = 0.2;
    // controls total angular momentum


    // Simulation

    final static double DT = 0.002 ;  // Time step
    final static double G = 6.667 * Math.pow(10, -11);
    final static double AVG_DISTANCE = 3.784*Math.pow(10, 10);


    // Display

    final static int WINDOW_SIZE =800 ;
    final static int DELAY = 0 ;
    final static int OUTPUT_FREQ = 1 ;


    // Star positions
    static double [] positionsX = new double [N] ;
    static double [] positionsY = new double [N] ;
    static double [] positionsZ = new double [N] ;

    // Star velocities
    static double [] velocitiesX = new double [N] ;
    static double [] velocitiesY = new double [N] ;
    static double [] velocitiesZ = new double [N] ;

    // Star accelerations
    static double [] accelerationsX = new double [N] ;
    static double [] accelerationsY = new double [N] ;
    static double [] accelerationsZ = new double [N] ;

    // Star masses
    static double [] masses = new double [N];

    static CyclicBarrier barrier = new CyclicBarrier(P);
    private static Display display = new Display();
    int me;

    public PhysicsGravity(int me){
        this.me = me;
    }

    public static void main(String args []) throws Exception {

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

            Random r = new Random();
            double min = 1.65 * Math.pow(10, 29);
            double max = 2.983 * Math.pow(10, 32);
            masses[i] = r.nextDouble()*(max - min) + min;
        }

        PhysicsGravity[] threads = new PhysicsGravity[P];
        for(int i = 0; i < P; i++){
            threads[i] = new PhysicsGravity(i);
            threads[i].start();
        }

        long startTime = System.currentTimeMillis();

        for(int i = 0; i < P; i++){
            threads[i].join();
        }

        System.out.println("Calculation completed in "
                + (System.currentTimeMillis() - startTime) + " milliseconds");

    }

    static void synch() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    final static int B = N / P ;

    public void run(){
        int iter = 0 ;
        int begin = me * B ;
        int end = begin + B ;

        while(true){
            if(iter % OUTPUT_FREQ == 0 && me == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                display.repaint() ;
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }

            double dtOver2 = 0.5 * DT;
            double dtSquaredOver2 = 0.5 * DT * DT;
            for (int i = begin; i < end; i++) {
                // update position
                positionsX[i] += (velocitiesX[i] * DT) + (accelerationsX[i] * dtSquaredOver2);
                positionsY[i] += (velocitiesY[i] * DT) + (accelerationsY[i] * dtSquaredOver2);
                positionsZ[i] += (velocitiesZ[i] * DT) + (accelerationsZ[i] * dtSquaredOver2);
                // update velocity halfway
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }
            synch();



            computeAccelerations(begin, end);
            synch();




            for (int i = begin; i < end; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }
            synch();

            iter++;
        }
    }

    // Compute accelerations of all stars from current positions:
    static void computeAccelerations(int begin, int end) {

        double distanceX, distanceY, distanceZ;  // separations in positionsX and positionsY directions
        double distanceXSqr, distanceYSqr, distanceZSqr, rSquared, r, rCubedInv, fx, fy, fz;

        for (int i = 0; i < N; i++) {
            accelerationsX[i] = 0.0;
            accelerationsY[i] = 0.0;
            accelerationsZ[i] = 0.0;
        }



        // Interaction forces (gravity)
        // This is where the program spends most of its time.

        // (NOTE: use of Newton's 3rd law below to essentially half number
        // of calculations needs some care in a parallel version.
        // A naive decomposition on the i loop can lead to a race condition
        // because you are assigning to accelerationsX[j], etc.
        // You can remove these assignments and extend the j loop to a fixed
        // upper bound of N, or, for extra credit, find a cleverer solution!)

        for (int i = begin; i < end; i++) {
            for (int j = 0; j < N; j++) {  // loop over all distinct pairs
                if (i != j) {
                    // Vector version of inverse square law
                    distanceX = (positionsX[i] - positionsX[j]) * AVG_DISTANCE;
                    distanceY = (positionsY[i] - positionsY[j]) * AVG_DISTANCE;
                    distanceZ = (positionsZ[i] - positionsZ[j]) * AVG_DISTANCE;
                    distanceXSqr = distanceX * distanceX;
                    distanceYSqr = distanceY * distanceY;
                    distanceZSqr = distanceZ * distanceZ;
                    rSquared = distanceXSqr + distanceYSqr + distanceZSqr;
                    r = Math.sqrt(rSquared);
                    rCubedInv = 1.0 / (rSquared * r);
                    fx = -rCubedInv * distanceX * masses[i] * masses[j] * G;
                    fy = -rCubedInv * distanceY * masses[i] * masses[j] * G;
                    fz = -rCubedInv * distanceZ * masses[i] * masses[j] * G;

                    accelerationsX[i] += fx/masses[i];  // add this force on to i's acceleration (mass = 1)
                    accelerationsY[i] += fy/masses[i];
                    accelerationsZ[i] += fz/masses[i];
//                accelerationsX[j] -= fx;  // Newton's 3rd law
//                accelerationsY[j] -= fy;
//                accelerationsZ[j] -= fz;
                }
            }
        }
    }


    static class Display extends JPanel {

        static final double SCALE = WINDOW_SIZE / BOX_WIDTH ;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE)) ;

            JFrame frame = new JFrame("MD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            g.setColor(Color.WHITE) ;
            for(int i = 0 ; i < N ; i++) {
                int gx = (int) (SCALE * positionsX[i]) ;
                int gy = (int) (SCALE * positionsY[i]) ;
                if(0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) {
                    g.fillRect(gx, gy, 1, 1) ;
                }
            }
        }
    }
}
    
