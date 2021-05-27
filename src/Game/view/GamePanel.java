package Game.view;

import Game.Collider;
import Game.LevelData;
import Pea.model.FreezePea;
import Pea.model.NormalPea;
import plant.model.FreezePeashooter;
import plant.model.Peashooter;
import plant.model.Plant;
import plant.model.Sunflower;
import zombie.model.ConeHeadZombie;
import zombie.model.NormalZombie;
import zombie.model.Zombie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends JLayeredPane {

  private static GamePanel gamePanel = null;

  private Image backgroundImage;
  private Image sunflowerImage;
  private Image peashooterImage;
  private Image freezePeashooterImage;

  private Image normalZombieImage;
  private Image coneHeadZombieImage;
  private Collider[] colliders;

  private ArrayList<ArrayList<Zombie>> laneZombies;
  private ArrayList<ArrayList<NormalPea>> lanePeas;

  private Timer redrawTimer;
  private Timer advancerTimer;
  private Timer zombieProducer;
  private JLabel sunScoreboard;

  private GameWindow.PlantType activePlantingBrush = GameWindow.PlantType.None;

  private int sunScore;


  private GamePanel() {
    JLabel sun = new JLabel("SUN");
    sun.setLocation(37, 80);
    sun.setSize(60, 20);

    initializeLayout(sun);
    loadImages();

    initializeLaneZombies();
    initializeLanePeas();
    initializeCollider();
    setSunScore(150); // pool avalie


    setRedrawTimer();
    setAdvancerTimer();
    setZombieProducerTimer();
  }

  public static GamePanel getInstance() {
    if (gamePanel == null) {
      gamePanel = new GamePanel();
    }
    return gamePanel;
  }

  private void setZombieProducerTimer() {
    zombieProducer = new Timer(7000, (ActionEvent e) -> {
      Random random = new Random();
      LevelData lvl = new LevelData();
      String[] Level = lvl.LEVEL_CONTENT[Integer.parseInt(lvl.LEVEL_NUMBER) - 1];
      int[][] LevelValue = lvl.LEVEL_VALUE[Integer.parseInt(lvl.LEVEL_NUMBER) - 1];
      int randomLine = random.nextInt(5);
      int randomTime = random.nextInt(100);
      Zombie z = null;
      for (int i = 0; i < LevelValue.length; i++) {
        if (randomTime >= LevelValue[i][0] && randomTime <= LevelValue[i][1]) {
          z = Zombie.getZombie(Level[i], GamePanel.this, randomLine);
        }
      }
      laneZombies.get(randomLine).add(z);
    });
    zombieProducer.start();
  }

  private void setAdvancerTimer() {
    advancerTimer = new Timer(60, (ActionEvent e) -> advance());
    advancerTimer.start();
  }

  private void setRedrawTimer() {
    redrawTimer = new Timer(25, (ActionEvent e) -> {
      repaint();
    });
    redrawTimer.start();
  }

  private void initializeCollider() {
    colliders = new Collider[45];
    for (int i = 0; i < 45; i++) {
      Collider collider = new Collider();
      collider.setLocation(44 + (i % 9) * 100, 109 + (i / 9) * 120);
      collider.setAction(new PlantActionListener((i % 9), (i / 9)));
      colliders[i] = collider;
      add(collider, new Integer(0));
    }
  }

  private void initializeLanePeas() {
    lanePeas = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      lanePeas.add(new ArrayList<>());
    }
  }

  private void initializeLaneZombies() {
    laneZombies = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      laneZombies.add(new ArrayList<>());
    }
  }

  private void initializeLayout(JLabel sunScoreboard) {
    setSize(1000, 752);
    setLayout(null);
    this.sunScoreboard = sunScoreboard;
    add(sunScoreboard, new Integer(2));
  }

  private void loadImages() {
    backgroundImage = new ImageIcon(this.getClass().getResource("../../images/mainBG.png")).getImage();
    peashooterImage =
        new ImageIcon(this.getClass().getResource("../../images/plants/peashooter.gif")).getImage();
    freezePeashooterImage =
        new ImageIcon(this.getClass().getResource("../../images/plants/freezepeashooter.gif"))
            .getImage();
    sunflowerImage =
        new ImageIcon(this.getClass().getResource("../../images/plants/sunflower.gif")).getImage();

    normalZombieImage =
        new ImageIcon(this.getClass().getResource("../../images/zombies/zombie1.png")).getImage();
    coneHeadZombieImage =
        new ImageIcon(this.getClass().getResource("../../images/zombies/zombie2.png")).getImage();
  }

  private void advance() {
    for (int laneIndex = 0; laneIndex < 5; laneIndex++) {
      for (Zombie zombies : laneZombies.get(laneIndex)) {
        zombies.advance();
        if (zombies.getPosX() < 0) {
          gameOver();
        }
        if (!zombies.getAlive()) {
          killZombie(laneIndex, zombies);
          break;
        }
      }
      peaAdvance(laneIndex);

      for (Collider c : colliders) {
        if (c.getPlant() != null && c.getPlant().getHealth() <= 0) {
          c.removePlant();
        }
      }

    }
  }

  private void peaAdvance(int laneIndex) {
    for (int j = 0; j < lanePeas.get(laneIndex).size(); j++) {
      NormalPea pea = lanePeas.get(laneIndex).get(j);
      Rectangle peaRectangle =
          new Rectangle(pea.getXPosition(), 130 + pea.getMyLane() * 120, 28, 28);
      for (int zombieIndex = 0; zombieIndex < gamePanel.getLaneZombies().get(pea.getMyLane())
          .size(); zombieIndex++) {
        Zombie zombie = gamePanel.getLaneZombies().get(pea.getMyLane()).get(zombieIndex);
        Rectangle zombieRectangle =
            new Rectangle(zombie.getPosX(), 109 + pea.getMyLane() * 120, 400, 120);
        if (peaRectangle.intersects(zombieRectangle)) {
          zombie.setHealth(zombie.getHealth() - 300);
          if (pea instanceof FreezePea)
            zombie.slow();
          boolean exit = false;

          gamePanel.getLaneZombies().get(pea.getMyLane()).remove(pea);
          if (exit)
            break;
        }
      }
      /*
       * if(posX > 2000){ gp.lanePeas.get(myLane).remove(this); }
       */
      pea.advance();
    }
  }

  private void killZombie(int i, Zombie z) {
    System.out.println("ZOMBIE DIED");
    laneZombies.get(i).remove(z);
    setProgress(10);
  }

  private void gameOver() {
    JOptionPane.showMessageDialog(this,
        "ZOMBIES ATE YOUR BRAIN !" + '\n' + "Starting the level again");
    gamePanel = null;
    GameWindow.gameWindow.dispose();
    GameWindow.gameWindow = new GameWindow();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(backgroundImage, 0, 0, null);

    // Draw Plants
    for (int i = 0; i < 45; i++) {
      Collider c = colliders[i];
      if (c.getPlant() != null) {
        Plant p = c.getPlant();
        if (p instanceof Peashooter) {
          g.drawImage(peashooterImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
        }
        if (p instanceof FreezePeashooter) {
          g.drawImage(freezePeashooterImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
        }
        if (p instanceof Sunflower) {
          g.drawImage(sunflowerImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
        }
      }
    }

    for (int i = 0; i < 5; i++) {
      for (Zombie z : laneZombies.get(i)) {
        if (z instanceof NormalZombie) {
          g.drawImage(normalZombieImage, z.getPosX(), 109 + (i * 120), null);
        } else if (z instanceof ConeHeadZombie) {
          g.drawImage(coneHeadZombieImage, z.getPosX(), 109 + (i * 120), null);
        }
      }

      for (int j = 0; j < lanePeas.get(i).size(); j++) {
        NormalPea pea = lanePeas.get(i).get(j);
        g.drawImage(pea.getImage(), pea.getXPosition(), 130 + (i * 120), null);
      }

    }
  }

  private class PlantActionListener implements ActionListener {

    int x, y;

    public PlantActionListener(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (activePlantingBrush == GameWindow.PlantType.Sunflower) {
        if (getSunScore() >= 50) {
          colliders[x + y * 9].setPlant(new Sunflower(x, y));
          setSunScore(getSunScore() - 50);
        }
      }
      if (activePlantingBrush == GameWindow.PlantType.Peashooter) {
        if (getSunScore() >= 100) {
          colliders[x + y * 9].setPlant(new Peashooter(x, y));
          setSunScore(getSunScore() - 100);
        }
      }

      if (activePlantingBrush == GameWindow.PlantType.FreezePeashooter) {
        if (getSunScore() >= 175) {
          colliders[x + y * 9].setPlant(new FreezePeashooter(x, y));
          setSunScore(getSunScore() - 175);
        }
      }
      activePlantingBrush = GameWindow.PlantType.None;
    }
  }

  static int progress = 0;

  public static void setProgress(int num) {
    progress = progress + num;
    System.out.println(progress);
    if (progress >= 150) {
      if ("1".equals(LevelData.LEVEL_NUMBER)) {
        JOptionPane.showMessageDialog(null,
            "LEVEL_CONTENT Completed !!!" + '\n' + "Starting next LEVEL_CONTENT");
        gamePanel = null;
        GameWindow.gameWindow.dispose();
        LevelData.write("2");
        GameWindow.gameWindow.gameStart();
      } else {
        JOptionPane.showMessageDialog(null, "LEVEL_CONTENT Completed !!!" + '\n'
            + "More Levels will come soon !!!" + '\n' + "Resetting data");
        LevelData.write("1");
        System.exit(0);
      }
      progress = 0;
    }
  }

  public GameWindow.PlantType getActivePlantingBrush() {
    return activePlantingBrush;
  }

  public void setActivePlantingBrush(GameWindow.PlantType activePlantingBrush) {
    this.activePlantingBrush = activePlantingBrush;
  }

  public ArrayList<ArrayList<Zombie>> getLaneZombies() {
    return laneZombies;
  }

  public void setLaneZombies(ArrayList<ArrayList<Zombie>> laneZombies) {
    this.laneZombies = laneZombies;
  }

  public ArrayList<ArrayList<NormalPea>> getLanePeas() {
    return lanePeas;
  }

  public void setLanePeas(ArrayList<ArrayList<NormalPea>> lanePeas) {
    this.lanePeas = lanePeas;
  }

  public Collider[] getColliders() {
    return colliders;
  }

  public void setColliders(Collider[] colliders) {
    this.colliders = colliders;
  }

  public int getSunScore() {
    return sunScore;
  }

  public void setSunScore(int sunScore) {
    this.sunScore = sunScore;
    sunScoreboard.setText(String.valueOf(sunScore));
  }
}
