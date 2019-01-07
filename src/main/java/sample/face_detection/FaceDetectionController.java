package sample.face_detection;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextFlow;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
//import org.opencv.videoio.VideoCapture;

//import org.opencv.highgui.VideoCapture;


import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import org.opencv.videoio.VideoCapture;



import sample.utils.Utils;

/**
 * The controller associated with the only view of our application. The
 * application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative
 * controls and the face detection/tracking.
 *
 *
 * Start - stop the camera - acquired video stream
 * Control face detection / tracking


 FaceDetectionController - used for detect face - controller

 // add - Object detection
 */
public class FaceDetectionController
{

    // File location path where the face will be saved and retrieved

    public String filePath="./faces";

    // FXML buttons
    @FXML
    private Button cameraButton;
    // the FXML area for showing the current frame
    @FXML
    private ImageView originalFrame;
    // checkboxes for enabling/disabling a classifier
    @FXML
    private CheckBox haarClassifier;
    @FXML
    private CheckBox lbpClassifier;


    /**
     * Object detector FXML
     */

    @FXML
    private Button startCam;
    @FXML
    private Button stopBtn;
    @FXML
    private Button motionBtn;
    @FXML
    private Button eyeBtn;
    @FXML
    private Button shapeBtn;
    @FXML
    private Button upperBodyBtn;
    @FXML
    private Button fullBodyBtn;
    @FXML
    private Button smileBtn;
    @FXML
    private Button gesture;
    @FXML
    private Button gestureStop;
    @FXML
    private Button saveBtn;
    @FXML
    private Button ocrBtn;
    @FXML
    private Button capBtn;
    @FXML
    private Button recogniseBtn;
    @FXML
    private Button stopRecBtn;
    @FXML
    private ImageView frame;
    @FXML
    private ImageView motionView;
    @FXML
    private AnchorPane pdPane;
    @FXML
    private TitledPane dataPane;
    @FXML
    private TextField fname;
    @FXML
    private TextField lname;
    @FXML
    private TextField code;
    @FXML
    private TextField reg;
    @FXML
    private TextField sec;
    @FXML
    private TextField age;
    @FXML
    public ListView<String> logList;
    @FXML
    public ListView<String> output;
    @FXML
    public ProgressIndicator pb;
    @FXML
    public Label savedLabel;
    @FXML
    public Label warning;
    @FXML
    public Label title;
    @FXML
    public TilePane tile;
    @FXML
    public TextFlow ocr;


    /**
     * For object detector
     */


    FaceDetector faceDetect = new FaceDetector();	//Creating Face detector object
    ColoredObjectTracker cot = new ColoredObjectTracker(); //Creating Color Object Tracker object
    Database database = new Database();		//Creating Database object

    OCR ocrObj = new OCR();
    ArrayList<String> user = new ArrayList<String>();
    ImageView imageView1;

    public static ObservableList<String> event = FXCollections.observableArrayList();
    public static ObservableList<String> outEvent = FXCollections.observableArrayList();

    public boolean enabled = false;
    public boolean isDBready = false;


    /**
     * End for object detector
     */
    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that performs the video capture
    private VideoCapture capture;
    // a flag to change the button behavior
    private boolean cameraActive;

    // face cascade classifier
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;

    /**
     * Init the controller, at start time
     * 1. Video capture
     * 2. Face cascade
     * 3. Set fixed width for the frame
     * 4. Preserve image ratio
     */
    protected void init()
    {
        System.out.print("I N I T ");


        this.capture = new VideoCapture();

        this.faceCascade = new CascadeClassifier();
        this.absoluteFaceSize = 0;

        // set a fixed width for the frame
        originalFrame.setFitWidth(600);
        // preserve image ratio
        originalFrame.setPreserveRatio(true);
    }

    /**
     * The action triggered by pushing the button on the GUI
     *
     * Start
     */
    @FXML
    protected void startCamera()
    {
        System.out.print("START CAMERA - STOP CAMERA FUNCTIONS");

        if (!this.cameraActive)
        {
            // disable setting checkboxes
            this.haarClassifier.setDisable(true);
            this.lbpClassifier.setDisable(true);

            // start the video capture
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened())
            {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run()
                    {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();
                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(originalFrame, imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);



                 // Set the button content From Start Camera -> Stop Camera

                this.cameraButton.setText("Stop Camera");
            }
            else
            {
                // log the error
                System.err.println("Failed to open the camera connection...");
            }
        }
        else
        {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");
            // enable classifiers checkboxes
            this.haarClassifier.setDisable(false);
            this.lbpClassifier.setDisable(false);

            // stop the timer
            this.stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Image} to show
     */
    private Mat grabFrame()
    {
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened())
        {
            try
            {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty())
                {
                    // face detection
                    this.detectAndDisplay(frame);
                }

            }
            catch (Exception e)
            {
                // log the (full) error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return frame;
    }

    /**
     * Method for face detection and tracking
     *
     *
     * Detect and Display function
     *
     * @param frame
     *            it looks for faces in this frame
     *
     *            1. Init Mat obj
     *            2. Convert the frame in gray scale
     *
     *            3. Equalize the frame histogram -> improve result
     *            4. Compute minimum face size
     *
     *            5. Detect face - algorithm
     *
     *
     *            6. Draw each detect face a rectangle
     */
    private void detectAndDisplay(Mat frame)
    {
        System.out.print("DETECTING.....");

        MatOfRect faces = new MatOfRect();

        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);

    }

    /**
     * The action triggered by selecting the Haar Classifier checkbox. It loads
     * the trained set to be used for frontal face detection.
     */
    @FXML
    protected void haarSelected(Event event)
    {
        // check whether the lpb checkbox is selected and deselect it
        if (this.lbpClassifier.isSelected())
            this.lbpClassifier.setSelected(false);

        this.checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");
    }

    /**
     * The action triggered by selecting the LBP Classifier checkbox. It loads
     * the trained set to be used for frontal face detection.
     */
    @FXML
    protected void lbpSelected(Event event)
    {
        // check whether the haar checkbox is selected and deselect it
        if (this.haarClassifier.isSelected())
            this.haarClassifier.setSelected(false);

        this.checkboxSelection("resources/lbpcascades/lbpcascade_frontalface.xml");
    }

    /**
     * Method for loading a classifier trained set from disk
     *
     * @param classifierPath
     *            the path on disk where a classifier trained set is located
     */
    private void checkboxSelection(String classifierPath)
    {
        // load the classifier(s)
        this.faceCascade.load(classifierPath);

        // now the video capture can start
        this.cameraButton.setDisable(false);
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition()
    {
        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view
     *            the {@link ImageView} to update
     * @param image
     *            the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image)
    {
        // Always update the image view in the Java FX main thread
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed()
    {
        this.stopAcquisition();

    }


    // it did not log the error - what the hell

    // thanh nien nay hoan toan im lan g


}