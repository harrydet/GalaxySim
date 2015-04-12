
/*
 * "Physics" part of code adapted from Dan Schroeder's applet at:
 *
 *     http://physics.weber.edu/schroeder/software/mdapplet.html
 */



import java.awt.* ;
import javax.swing.* ;

public class GravitySequential {

    // Size of simulation

    final static int N = 2000 ;  // Number of "stars"
    final static double BOX_WIDTH = 100.0 ;


    // Initial state

    final static double RADIUS = 20.0 ;  // of randomly populated sphere

    final static double ANGULAR_VELOCITY = 0.4 ;
    // controls total angular momentum


    // Simulation

    final static double DT = 0.002 ;  // Time step


    // Display

    final static int WINDOW_SIZE = 800 ;
    final static int DELAY = 0 ;
    final static int OUTPUT_FREQ = 2 ;


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


    public static void main(String args []) throws Exception {

        Display display = new Display() ;

        // Define initial state of stars

        /*

        // Randomly choose plane for net angular velocity

        double nx = 2 * Math.random() - 1 ;
        double ny = 2 * Math.random() - 1 ;
        double nz = 2 * Math.random() - 1 ;
        double norm = 1.0 / Math.sqrt(nx * nx + ny * ny + nz * nz) ;
        nx *= norm ;
        ny *= norm ;
        nz *= norm ;

        */

        // ... or just rotate in positionsX, positionsY plane
        double nx = 0, ny = 0, nz = 1.0 ;

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
        int iter = 0 ;
        while(iter < 500) {

            if(iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                display.repaint() ;
                Thread.sleep(DELAY) ;
            }

            // Verlet integration:
            // http://en.wikipedia.org/wiki/Verlet_integration#Velocity_Verlet

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

            computeAccelerations();

            for (int i = 0; i < N; i++) {
                // finish updating velocity with new acceleration
                velocitiesX[i] += (accelerationsX[i] * dtOver2);
                velocitiesY[i] += (accelerationsY[i] * dtOver2);
                velocitiesZ[i] += (accelerationsZ[i] * dtOver2);
            }

            iter++ ;
        }
        System.out.println("Calculation completed in "
                + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

    // Compute accelerations of all stars from current positions:
    static void computeAccelerations() {

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

        for (int i = 1; i < N; i++) {
            for (int j = 0; j < i; j++) {  // loop over all distinct pairs

                // Vector version of inverse square law
                distanceX = positionsX[i] - positionsX[j];
                distanceY = positionsY[i] - positionsY[j];
                distanceZ = positionsZ[i] - positionsZ[j];
                distanceXSqr = distanceX * distanceX;
                distanceYSqr = distanceY * distanceY;
                distanceZSqr = distanceZ * distanceZ;
                rSquared = distanceXSqr + distanceYSqr + distanceZSqr ;
                r = Math.sqrt(rSquared) ;
                rCubedInv = 1.0 / (rSquared * r) ;
                fx = - rCubedInv * distanceX;
                fy = - rCubedInv * distanceY;
                fz = - rCubedInv * distanceZ;

                accelerationsX[i] += fx;  // add this force on to i's acceleration (mass = 1)
                accelerationsY[i] += fy;
                accelerationsZ[i] += fz;
                accelerationsX[j] -= fx;  // Newton's 3rd law
                accelerationsY[j] -= fy;
                accelerationsZ[j] -= fz;
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
    
