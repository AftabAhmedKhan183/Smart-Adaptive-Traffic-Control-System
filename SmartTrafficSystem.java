import java.awt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

// ================= TrafficLight Module =================
class TrafficLight {
    public enum State { RED, YELLOW, GREEN }
    private State state;
    public TrafficLight() { state = State.RED; }
    public synchronized void set(State s) { state = s; }
    public synchronized State get() { return state; }
}

// ================= Vehicle Module =================
class Vehicle {
    protected String type;
    public Vehicle(String type) { this.type = type; }
    public boolean isEmergency() { return false; }
    public String getType() { return type; }
}

class EmergencyVehicle extends Vehicle {
    public EmergencyVehicle() { super("EMERGENCY"); }
    @Override public boolean isEmergency() { return true; }
}

// ================= Lane Module =================
class Lane {
    private final String name;
    private int vehicleCount = 0;
    private int crowdCount = 0;
    private boolean emergencyPresent = false;
    private final TrafficLight light = new TrafficLight();


    public Lane(String name) { this.name = name; }

    public synchronized void addVehicle(Vehicle v) {
        vehicleCount++;
        if (v.isEmergency()) emergencyPresent = true;
    }

    public synchronized void addVehicles(int n) { vehicleCount += n; }
    public synchronized void addCrowd(int n) { crowdCount += n; }
    public synchronized void clearCounts() { vehicleCount = 0; crowdCount = 0; emergencyPresent = false; }

    public synchronized int getVehicleCount() { return vehicleCount; }
    public synchronized int getCrowdCount() { return crowdCount; }
    public synchronized boolean hasEmergency() { return emergencyPresent; }
    public synchronized int getLoad() { return vehicleCount + crowdCount; }
    public TrafficLight getLight() { return light; }
    public String getName() { return name; }

    // Reduce counts after green signal
    public synchronized void reduceCountsAfterGreen() {
        int reduceVehicles = Math.max(1, vehicleCount / 3);
        vehicleCount = Math.max(0, vehicleCount - reduceVehicles);
        int reduceCrowd = Math.min(5, crowdCount);
        crowdCount = Math.max(0, crowdCount - reduceCrowd);
        // emergency remains intact until manually cleared
    }


}

// ================= SmartControl Module =================
class SmartControl {
    public int calculateGreenTime(Lane lane) {
        if (lane.hasEmergency()) return 8;
        int load = lane.getLoad();
        if (load >= 60) return 30;
        if (load >= 40) return 20;
        if (load >= 15) return 12;
        if (load >= 5) return 8;
        return 5;
    }


    public int priorityScore(Lane lane) {
        int score = lane.getLoad();
        if (lane.hasEmergency()) score += 1000;
        return score;
    }


}

// ================= Display Module =================
class TrafficDisplay extends JFrame {
    private final LightPanel northPanel, eastPanel, southPanel, westPanel;
    private final JLabel infoLabel;
    private final Map<String, JButton> addVehicleButtons = new HashMap<>();
    private final Map<String, JButton> addCrowdButtons = new HashMap<>();
    private final Map<String, JButton> addEmergencyButtons = new HashMap<>();


    public TrafficDisplay() {
        super("Smart Traffic Control System - GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLayout(new BorderLayout(8, 8));

        infoLabel = new JLabel("Status: Initializing...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(infoLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        northPanel = new LightPanel("NORTH");
        eastPanel = new LightPanel("EAST");
        southPanel = new LightPanel("SOUTH");
        westPanel = new LightPanel("WEST");
        grid.add(northPanel); grid.add(eastPanel); grid.add(southPanel); grid.add(westPanel);
        add(grid, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createTitledBorder("Simulator Controls"));

        for (String dir : new String[]{"NORTH", "EAST", "SOUTH", "WEST"}) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lbl = new JLabel(dir + ": ");
            JButton vBtn = new JButton("Add Vehicle");
            JButton cBtn = new JButton("Add Crowd(5)");
            JButton eBtn = new JButton("Add Emergency");

            addVehicleButtons.put(dir, vBtn);
            addCrowdButtons.put(dir, cBtn);
            addEmergencyButtons.put(dir, eBtn);

            row.add(lbl); row.add(vBtn); row.add(cBtn); row.add(eBtn);
            controls.add(row);
        }

        controls.add(Box.createVerticalStrut(10));
        JButton resetBtn = new JButton("Reset All Counts");
        controls.add(resetBtn);
        add(controls, BorderLayout.EAST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(new JLabel("Legend: RED top | YELLOW middle | GREEN bottom"));
        add(bottom, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);

        resetBtn.addActionListener(e -> setInfo("Reset requested. Controller will handle reset."));
    }

    public JButton getAddVehicleButton(String dir) { return addVehicleButtons.get(dir); }
    public JButton getAddCrowdButton(String dir) { return addCrowdButtons.get(dir); }
    public JButton getAddEmergencyButton(String dir) { return addEmergencyButtons.get(dir); }
    public void setInfo(String text) { SwingUtilities.invokeLater(() -> infoLabel.setText(text)); }

    public void updateLaneUI(Lane lane) {
        SwingUtilities.invokeLater(() -> {
            LightPanel p = getPanelFor(lane.getName());
            if (p != null) {
                p.setCounts(lane.getVehicleCount(), lane.getCrowdCount(), lane.hasEmergency());
                p.setState(lane.getLight().get());
            }
        });
    }

    private LightPanel getPanelFor(String name) {
        switch (name) {
            case "NORTH": return northPanel;
            case "EAST": return eastPanel;
            case "SOUTH": return southPanel;
            default: return westPanel;
        }
    }

    class LightPanel extends JPanel {
        private final JLabel title;
        private final JPanel red, yellow, green;
        private final JLabel counts;

        LightPanel(String name) {
            setLayout(new BorderLayout(4, 4));
            title = new JLabel(name, SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 18));
            add(title, BorderLayout.NORTH);

            JPanel lights = new JPanel(new GridLayout(3, 1, 6, 6));
            red = makeLampPanel(Color.DARK_GRAY);
            yellow = makeLampPanel(Color.DARK_GRAY);
            green = makeLampPanel(Color.DARK_GRAY);
            lights.add(red); lights.add(yellow); lights.add(green);
            add(lights, BorderLayout.CENTER);

            counts = new JLabel("<html>Vehicles: 0<br/>Crowd: 0<br/>Emergency: NO</html>", SwingConstants.CENTER);
            add(counts, BorderLayout.SOUTH);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }

        private JPanel makeLampPanel(Color c) { JPanel p = new JPanel(); p.setBackground(c); return p; }

        public void setState(TrafficLight.State s) {
            red.setBackground(s == TrafficLight.State.RED ? Color.RED : Color.DARK_GRAY);
            yellow.setBackground(s == TrafficLight.State.YELLOW ? Color.YELLOW : Color.DARK_GRAY);
            green.setBackground(s == TrafficLight.State.GREEN ? Color.GREEN : Color.DARK_GRAY);
        }

        public void setCounts(int vehicles, int crowd, boolean emergency) {
            counts.setText("<html>Vehicles: " + vehicles + "<br/>Crowd: " + crowd + "<br/>Emergency: " + (emergency ? "YES" : "NO") + "</html>");
        }
    }


}

// ================= TrafficController Module =================
class TrafficController {
    private final Lane[] lanes;
    private final SmartControl smart;
    private final TrafficDisplay display;
    private final String[] order = {"NORTH", "EAST", "SOUTH", "WEST"};
    private final AtomicBoolean emergencyPreempt = new AtomicBoolean(false);
    private volatile boolean running = true;


    public TrafficController(Lane[] lanes, TrafficDisplay display) {
        this.lanes = lanes;
        this.display = display;
        this.smart = new SmartControl();
        attachGuiHandlers();
        for (Lane l : lanes) display.updateLaneUI(l);
    }

    private void attachGuiHandlers() {
        for (Lane lane : lanes) {
            String dir = lane.getName();
            display.getAddVehicleButton(dir).addActionListener(evt -> { lane.addVehicle(new Vehicle("CAR")); display.updateLaneUI(lane); display.setInfo("Added vehicle to " + dir); });
            display.getAddCrowdButton(dir).addActionListener(evt -> { lane.addCrowd(5); display.updateLaneUI(lane); display.setInfo("Added crowd(5) to " + dir); });
            display.getAddEmergencyButton(dir).addActionListener(evt -> { lane.addVehicle(new EmergencyVehicle()); display.updateLaneUI(lane); display.setInfo("Emergency vehicle added to " + dir); emergencyPreempt.set(true); });
        }
    }

    public void start() {
        Thread t = new Thread(this::controlLoop, "TrafficControllerThread");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; }

    private void controlLoop() {
        int idx = 0;
        display.setInfo("Controller started. Round-robin mode.");
        while (running) {
            try {
                Lane lane = lanes[idx % 4];
                Lane emergencyLane = Arrays.stream(lanes).filter(Lane::hasEmergency).findFirst().orElse(null);
                if (emergencyLane != null && !emergencyLane.getName().equals(lane.getName())) {
                    serveLane(emergencyLane);
                    idx = (indexOf(emergencyLane.getName()) + 1) % 4;
                    emergencyPreempt.set(false);
                    continue;
                }
                serveLane(lane);
                idx = (idx + 1) % 4;
                Thread.sleep(200);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }
    }

    private void serveLane(Lane lane) throws InterruptedException {
        for (Lane l : lanes) { l.getLight().set(TrafficLight.State.RED); display.updateLaneUI(l); }
        display.setInfo("Preparing to release: " + lane.getName());
        lane.getLight().set(TrafficLight.State.RED); display.updateLaneUI(lane); Thread.sleep(600);
        lane.getLight().set(TrafficLight.State.YELLOW); display.updateLaneUI(lane); Thread.sleep(600);
        lane.getLight().set(TrafficLight.State.GREEN); display.updateLaneUI(lane);

        int greenSeconds = smart.calculateGreenTime(lane);
        long endTime = System.currentTimeMillis() + greenSeconds * 1000L;
        while (System.currentTimeMillis() < endTime) {
            display.setInfo("GREEN: " + lane.getName() + " | remaining(s): " + ((endTime - System.currentTimeMillis())/1000) + " | vehicles: " + lane.getVehicleCount() + " crowd: " + lane.getCrowdCount());
            display.updateLaneUI(lane);
            if (emergencyPreempt.get()) break;
            Thread.sleep(1000);
        }

        lane.getLight().set(TrafficLight.State.YELLOW); display.updateLaneUI(lane); Thread.sleep(1500);
        lane.getLight().set(TrafficLight.State.RED); display.updateLaneUI(lane);
        lane.reduceCountsAfterGreen();
        display.updateLaneUI(lane);
    }

    private int indexOf(String name) { for (int i=0;i<4;i++) if (lanes[i].getName().equals(name)) return i; return 0; }


}

// ================= Main Module =================
public class SmartTrafficSystem {
    public static void main(String[] args) {
        Lane north = new Lane("NORTH");
        Lane east = new Lane("EAST");
        Lane south = new Lane("SOUTH");
        Lane west = new Lane("WEST");
        Lane[] lanes = new Lane[]{north, east, south, west};


        north.addVehicles(8); north.addCrowd(10);
        east.addVehicles(3); east.addCrowd(4);
        south.addVehicles(6); south.addCrowd(12);
        west.addVehicles(2); west.addCrowd(1);

        SwingUtilities.invokeLater(() -> {
            TrafficDisplay display = new TrafficDisplay();
            TrafficController controller = new TrafficController(lanes, display);
            controller.start();
            for (Lane l : lanes) display.updateLaneUI(l);
        });
    }


}