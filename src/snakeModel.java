import java.awt.Point; //used as a class that can hold x and y cordinates
import java.util.ArrayList;
import java.util.Observable;
import java.util.Random;

/**
 * Created by Daniel Gude
 */
public class snakeModel extends Observable {

    final protected int DIM = 32;  //length and width of the playing area
    final protected int  STARTLENGTH = 3;  //length of tail to begin with
    final protected int NUMDOTS = 3;  //how many dots on the board at anytime
    final protected int NUMBLOCKS = 5;
    protected ArrayList<Point> tail = new ArrayList<Point>();  //cordinates of tail
    protected ArrayList<Point> dots = new ArrayList<Point>();  //cordiantes of dots
    protected ArrayList<Point> blocks = new ArrayList<Point>();//cordiantes ofblocks
    private Random rand = new Random(); //used to create random columns for dots
    protected boolean lost = false;  //if the player has lost the game
    protected boolean won = false;
    protected boolean paused = false;  //wether the game is paused
    boolean stop = false;  //wether ther GUI has been closed
    protected int heading = 1;  //int to represent the direction heading
    //East: 0, North: 1, West: 2, South: 3
    private boolean canChangeHeading = true;  //allowed to change heading
    private ArrayList<Point> changes = new ArrayList<Point>();
    //changes in x and y dependent on using heading as index
    protected int score = 0; //current score of the game
    protected double speed = 160;

    protected snakeModel() {
        newStuff();
        //creates values for changes list
        for (int i=0; i < 4; i++) {
            changes.add(new Point((int)Math.cos(i*Math.PI/2),
                    (int)-Math.sin(i*Math.PI/2)));
        }
    }

    /**
     * sets up dots, blocks and puts the snake in starting position
     */
    private void newStuff(){
        int mid = DIM/2;
        for (int i = STARTLENGTH; i > 0; i--) {
            tail.add(new Point(mid, DIM-i));
        }
        for (int i=0; i < NUMBLOCKS; i++) {
            blocks.add(newBlock());
        }
        for (int i=0; i < NUMDOTS; i++) {
            dots.add(newDot());
        }
    }

    /**
     * sets values to be able to start the game agian
     */
    protected void reset() {
        lost = false;
        heading = 1;
        score = 0;
        tail = new ArrayList<Point>();
        dots = new ArrayList<Point>();
        blocks = new ArrayList<Point>();
        newStuff();
        updateGUI(false);
    }

    /**
     *if the dot is an invalid location tail recursion is used to make a new one
     * @return point for a dot
     */
    private Point newDot() {
        Point newP = new Point(
                rand.nextInt((DIM-1)+ 1), rand.nextInt((DIM-1)+ 1));
        int edges = 0;
        for (Point tailP : tail) {
            if (newP.distance(tailP) == 0){
                return newDot();
            }
        }
        for (Point dotP : dots) {
            if (newP.distance(dotP) == 0){
                return newDot();
            }

        }
        for (Point blockP : blocks) {
            double dist = newP.distance(blockP);
            if (dist == 0){
                return newDot();
            }
            else if (dist == 1){
                edges++;
            }
        }
        if (newP.x == 0 || newP.x == DIM-1){
            edges++;
        }
        if (newP.y == 0 || newP.y == DIM-1){
            edges++;
        }
        if (edges > 2){
            return newDot();
        }
        return newP;
    }

    /**
     *if the dot is an invalid location tail recursion is used to make a new one
     * @return point for a dot
     */
    private Point newBlock() {
        Point newP = new Point(
                rand.nextInt((DIM-1)+ 1), rand.nextInt((DIM-1)+ 1));
        if (newP.x == tail.get(0).x) {
            return newBlock();
        }
        for (Point blockP : blocks) {
            if (newP.distance(blockP) == 0){
                return newBlock();
            }
        }
        return newP;
    }

    /**
     * core method for running the game, runs on a loop as long as lost is false
     * sleep for a pause in moving one more space
     * goes into a loop is paused only to break out by becoming unpaused
     * checks if the snake is about to crash and lose the game
     * adds the next space to tail and removes last point to simulate movement
     * allows the heading to be changed
     * calls gotDot() and adds score if needed
     * updates GUI with current status of lost
     * @throws InterruptedException
     */
    protected void startGame() throws InterruptedException {
        while (!lost){
            Thread.sleep( (new Double(speed)).longValue() );
            while (paused || stop){
                if (stop) { Thread.currentThread().stop(); }
                Thread.sleep(10L);
            }

            Point head = tail.get(0);
            Point change = changes.get(heading);
            Point newP = new Point(head.x+change.x, head.y+change.y);

            if (didLose(newP)) {
                lost = true;
            }
            else {
                tail.add(0, newP);
                canChangeHeading = true;
                if (!gotDot(newP)) {
                    tail.remove(tail.size()-1);
                }
                else {
                    score++;
                }
            }
            updateGUI(lost);
        }
    }

    /**
     * player loses or not by reaching the edge or hitting the tail
     * @param newP point the snake is heading to in the current iteration
     * @return boolean
     */
    private boolean didLose(Point newP){
        if (newP.x < 0 ||  newP.x >= DIM || newP.y < 0 || newP.y >= DIM){
            return true;
        }
        for (Point tailP : tail) {
            if (newP.distance(tailP) == 0){
                return (newP.distance(tail.get(tail.size()-1)) > 0);
            }
        }
        for (Point blockP : blocks) {
            if (newP.distance(blockP) == 0){
                return true;
            }
        }

        return false;
    }

    /**
     * the snake just came up on a dot or not
     * removes it if it did and creates a new dot
     * @param newP the point the snake is coming up on
     * @return
     */
    private boolean gotDot(Point newP){
        for (Point dotP : dots) {
            if (newP.distance(dotP) == 0){
                try {
                    dots.add(newDot());
                }
                catch (StackOverflowError e){
                    won = true;
                    lost = true;
                }
                dots.remove(dotP);
                return true;
            }
        }
        return false;
    }

    /**
     * cant change heading while paused
     * cant turn directly around
     * sets canChangeHeading to false so the
     * heading can't be changed agian until the snake moves
     * @param h new heading value
     */
    protected void changeHeading(int h) {
        if (paused) {
            return;
        }
        if (canChangeHeading && heading != ((h+2)%4)){
            heading = h;
            canChangeHeading = false;
        }
    }

    /**
     * Protocol for observer update enforced by parent class
     * {@link java.util.Observable}
     * @param newVal if the game was lost
     */
    protected void updateGUI(Object newVal ) {
        super.setChanged();
        super.notifyObservers( newVal );
    }
}
