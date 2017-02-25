import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.Point;
import java.io.*;
import java.util.*;

/**
 * Created by Daniel Gude
 */
public class snake extends Application implements Observer {

    static snakeModel myModel = new snakeModel();
    // only possible texts for the button
    final String GAMECMDS[] = {"Start", "Pause", "Resume", "Reset",
            "Show Leader Board", "Hide Leader Board"};
    Button gameBttn = new Button(GAMECMDS[0]);
    Button secondBttn = new Button(GAMECMDS[4]);
    Text scoreTxt = new Text("");
    Text speedTxt = new Text("");
    Slider speedSldr = new Slider();
    ComboBox selector = new ComboBox();
    ArrayList<ArrayList<String>> all = new ArrayList<ArrayList<String>>();
    int change = 40;
    final int width = 20; //width of one square
    final int eyeWidth = width/4;
    private boolean reset = false;
    BorderPane mainPnl = new BorderPane();
    private String alertText = "Don't hit the wall, tail, and the red blocks." +
            "\nCollect the orange dots." +
            "\nUse Arrow Keys or WASD for turning." +
            "\nR to reset at anytime." +
            "\nThe slider is to control the dificulty:" +
            "\nLeft is harder, Right is easier.";

    public static void main(String[] args) {

        //launches the gui
        Application.launch( snake.class );

        //makes sure the executing thread stops if the gui is closed
        myModel.stop = true;
    }

    @Override
    /**
     * builds the base of the gui
     */
    public void start(Stage primaryStage) throws Exception {
        myModel.addObserver(this);

        selector.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<String>() {
                    public void changed(
                            ObservableValue<? extends String> observable,
                            String oldValue, String newValue) {
                        HBox newBot = new HBox();
                        newBot.getChildren().add(selector);
                        ArrayList<String> thisOne = all.get(
                                Integer.parseInt(newValue)/change);
                        Collections.sort(thisOne);
                        int s = thisOne.size();
                        if (s > 0) {
                            int max = 4;
                            if (max > s){ max = s; }
                            for (int i = 0; i < max; i++) {
                                String[] curr = thisOne.get(i).split(" ", 2);
                                newBot.getChildren().add(new VBox(
                                        new Text(curr[0]), new Text(curr[1])));
                            }
                        }
                        selector.setVisible(false);
                        mainPnl.setCenter(newBot);
                        selector.setVisible(true);
                    }
                });

        HBox top = new HBox(10);

        scoreTxt.fontProperty().setValue(Font.font(25));
        speedSldr.setMin(0);
        speedSldr.setMax(400);
        speedSldr.setValue(myModel.speed);
        speedSldr.setShowTickLabels(true);
        speedSldr.setMajorTickUnit(change);
        speedSldr.setMinorTickCount(0);
        speedSldr.setMinWidth(350);
        speedSldr.setShowTickMarks(false);
        speedSldr.setSnapToTicks(true);
        speedTxt.fontProperty().setValue(Font.font(18));
        speedTxt.setText(Double.toString(myModel.speed).substring(0, 3));
        top.getChildren().addAll(gameBttn, scoreTxt,
                speedSldr, speedTxt, secondBttn);

        speedSldr.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                double newSpeed = (double)new_val;
                speedTxt.setText(Double.toString(newSpeed).substring(0, 3));
                myModel.speed = speedSldr.getMax()-newSpeed +20;
            }
        });

        gameBttn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                handlE();
            }
        });

        secondBttn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                String curr =((Button) event.getSource()).getText();
                if (curr.equals(GAMECMDS[3])){
                    resetPressed();
                }
                else if (curr.equals(GAMECMDS[4])){
                    secondBttn.setText(GAMECMDS[5]);
                    mainPnl.setCenter(viewLeaderBoard());
                }
                else  if (curr.equals(GAMECMDS[5])){
                    secondBttn.setText(GAMECMDS[4]);
                    mainPnl.setCenter(drawBoard());
                }
            }
        });

        mainPnl.setTop(top);
        mainPnl.setCenter(drawBoard());
        Scene scene = new Scene(mainPnl);

        //controls for changing the heading
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                String curr = gameBttn.getText();
                switch (event.getCode()) {
                    case W:
                        if (!beginGame(curr)){
                            myModel.changeHeading(1);
                        }
                        break;
                    case A:
                        myModel.changeHeading(2);
                        break;
                    case S:
                        myModel.changeHeading(3);
                        break;
                    case D:
                        myModel.changeHeading(0);
                        break;
                    case UP:
                        if (!beginGame(curr)){
                            myModel.changeHeading(1);
                        }
                        break;
                    case LEFT:
                        myModel.changeHeading(2);
                        break;
                    case DOWN:
                        myModel.changeHeading(3);
                        break;
                    case RIGHT:
                        myModel.changeHeading(0);
                        break;
                    case SPACE:
                        handlE();
                        break;
                    case R:
                        if (curr.equals(GAMECMDS[0])){ }
                        else if (curr.equals(GAMECMDS[3])){
                            handlE();
                        }
                        else {
                            resetPressed();
                        }
                        break;
                }
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle( "Snake" );
        primaryStage.show();
        primaryStage.setResizable(false);

        for (int diff=0; diff <= speedSldr.getMax();
             diff+= speedSldr.getMajorTickUnit()) {
            all.add(diff/change, new ArrayList<>());
            selector.getItems().add(Integer.toString(diff));
        }

        //Alert for rules and controls
        Alert controls = new Alert(Alert.AlertType.INFORMATION);
        controls.setTitle("How to Play");
        controls.setHeaderText("Rules & Controls");
        controls.setContentText(alertText);
        controls.showAndWait();
    }

    private boolean beginGame(String curr){
        if (curr.equals(GAMECMDS[0])){
            gameBttn.setText(GAMECMDS[1]);
            secondBttn.setText(GAMECMDS[3]);
            speedSldr.setDisable(true);
            secondBttn.setVisible(true);
            new Thread( () -> {
                try {
                    myModel.startGame();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            return true;
        }
        return false;
    }

    private void resetPressed() {
        myModel.paused = false;
        myModel.lost = true;
        reset = true;
    }

    private void reset(){
        gameBttn.setText(GAMECMDS[0]);
        myModel.reset();
        speedSldr.setDisable(false);
        secondBttn.setVisible(true);
        secondBttn.setText(GAMECMDS[4]);
        reset = false;
    }

    /**
     * changes the button text accordingly
     * starts a new thread that runs the game
     */
    private void handlE() {
        String curr = gameBttn.getText();
        if (beginGame(curr)) {

        }
        else if (curr.equals(GAMECMDS[3])){
            reset();
        }
        else {
            myModel.paused = !myModel.paused;
            speedSldr.setDisable(true);
            if (curr.equals(GAMECMDS[1])){
                gameBttn.setText(GAMECMDS[2]);
            }
            else if (curr.equals(GAMECMDS[2])){
                gameBttn.setText(GAMECMDS[1]);
            }
        }
    }

    /**
     * stops if game was lost and shows message
     * changes the score text and draws the board agian
     * @param o
     * @param arg boolean if the game was lost
     */
    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater( () -> {
            if (reset){
                reset();
                return;
            }

            if ((boolean) arg)
            {
                gameBttn.setText(GAMECMDS[3]);
                secondBttn.setVisible(false);
                if (myModel.won){
                    Alert won = new Alert(Alert.AlertType.INFORMATION);
                    won.setTitle("Congratulations");
                    won.setHeaderText("You Won");
                    won.setContentText("Every possible dot has been gotten");
                    won.showAndWait();
                }
                else {
                    Alert lost = new Alert(Alert.AlertType.ERROR);
                    lost.setTitle("uh oh");
                    lost.setHeaderText("You Lost");
                    lost.setContentText(alertText);
                    lost.showAndWait();

                    TextInputDialog enterName = new TextInputDialog();
                    enterName.setTitle("LeaderBoard");
                    enterName.setHeaderText("Have your score, difficulty " +
                            "and name saved to the leader board");
                    enterName.setContentText("Please Enter your name:");
                    Optional<String> input = enterName.showAndWait();
                    if (input.isPresent()){
                        writeEntry(input.get());
                    }
                }

                return;
            }

            mainPnl.setCenter(drawBoard());
        });
    }

    private void writeEntry(String name) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            File file = new File("LeaderBoard.txt");

            if (!file.exists()) {
                file.createNewFile();
            }

            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            bw.write(String.format("%s %s %s\n",
                    speedTxt.getText(), myModel.score, name));

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Pane viewLeaderBoard() {
        ArrayList<ArrayList<String>> newAll = new ArrayList<ArrayList<String>>();


        for (int diff=0; diff <= speedSldr.getMax();
             diff+= speedSldr.getMajorTickUnit()) {
            newAll.add(diff/change, new ArrayList<>());
        }

        try{
            BufferedReader br = new BufferedReader(
                    new FileReader("LeaderBoard.txt"));

            String line = null;
            while ((line = br.readLine()) != null) {
                String[]lineA = line.split(" ", 2);
                newAll.get(Integer.parseInt(lineA[0])/change).add(lineA[1]);
            }
            all = newAll;
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HBox(selector);
    }

    /**
     * creates the background
     * then layers the snake ontop using myModel.tail
     * then adds dots using myModel.dots
     * @return GridePane using pulling new values from myModel
     */
    private GridPane drawBoard() {
        scoreTxt.setText(Integer.toString(myModel.score));

        GridPane board = new GridPane();

        //draws background
        for (int r=0; r < myModel.DIM; r++){
            for (int c=0; c < myModel.DIM; c++){
                Rectangle rect = new Rectangle(width, width);
                rect.setFill(Color.SKYBLUE);
                board.add(rect, c, r);
            }
        }

        //draws tail
        for (Point tailP : myModel.tail){
            int c = tailP.x;
            int r = tailP.y;
            Rectangle rect = new Rectangle(width, width);
            rect.setFill(Color.DARKGREEN);
            board.add(rect, c, r);
        }

        //draws head
        Rectangle headRct = new Rectangle(width, width);
        headRct.setFill(Color.GRAY);
        board.add(headRct, myModel.tail.get(0).x, myModel.tail.get(0).y);

        Rectangle eyeRctA = new Rectangle(eyeWidth, eyeWidth);
        eyeRctA.setFill(Color.BLACK);
        board.add(eyeRctA, myModel.tail.get(0).x, myModel.tail.get(0).y);

        Rectangle eyeRctB = new Rectangle(eyeWidth, eyeWidth);
        eyeRctB.setFill(Color.BLACK);
        board.add(eyeRctB, myModel.tail.get(0).x, myModel.tail.get(0).y);

        int heading = myModel.heading;
        int sixth = (width-(2*eyeWidth))/3;
        int wse = width -sixth -eyeWidth;
        int yOffset = ((width-eyeWidth)/2) + ((width-eyeWidth)%2);
        if (heading == 1 || heading == 2) {
            eyeRctA.setTranslateX(sixth);
            eyeRctA.setTranslateY(sixth - yOffset);
        }
        if (heading == 2 || heading == 3) {
            eyeRctB.setTranslateX(sixth);
            eyeRctB.setTranslateY(wse - yOffset);
        }
        if (heading == 3 || heading == 0) {
            eyeRctA.setTranslateX(wse);
            eyeRctA.setTranslateY(wse - yOffset);
        }
        if (heading == 0 || heading == 1) {
            eyeRctB.setTranslateX(wse);
            eyeRctB.setTranslateY(sixth - yOffset);
        }

        //draws dots
        for (Point dotP : myModel.dots){
            int c = dotP.x;
            int r = dotP.y;
            Rectangle rect = new Rectangle(width, width);
            rect.setFill(Color.DARKORANGE);
            board.add(rect, c, r);
        }

        //draws blocks
        for (Point blocksP : myModel.blocks){
            int c = blocksP.x;
            int r = blocksP.y;
            Rectangle rect = new Rectangle(width, width);
            rect.setFill(Color.CRIMSON);
            board.add(rect, c, r);
        }

        return board;
    }

}
