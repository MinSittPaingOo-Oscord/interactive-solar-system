

float simSpeed = 1.0;        // overall time multiplier
boolean paused = false;      // pause toggle  (key: SPACE)
float globalTime = 0;        // accumulated simulation time

// Camera / zoom
float camX = 0, camY = 0;   // pan offset
float zoom = 1.0;            // zoom level
int focusedPlanet = -1;      // index of planet being zoomed (-1 = none)
float focusZoom  = 1.0;      // animated zoom target

// Star-field particles
int   NUM_STARS = 300;
float[] starX, starY, starSize, starBright;

// Asteroid belt
int   NUM_ASTEROIDS = 180;
float[] astAngle, astRadius, astSize;

// Planet data arrays  (index 0 = Mercury … 7 = Neptune)
String[] planetName   = {"Mercury","Venus","Earth","Mars",
                          "Jupiter","Saturn","Uranus","Neptune"};

// Base orbit radii (pixels at zoom=1)
float[]  orbitR = {80, 120, 165, 215, 295, 375, 445, 510};

// Planet visual radii
float[]  pRadius = {6, 10, 11, 8, 26, 22, 17, 16};

// Orbital period multipliers (Earth = 1; smaller = faster)
float[]  period  = {0.24f, 0.62f, 1.0f, 1.88f,
                    11.86f, 29.46f, 84.01f, 164.8f};

// Planet colours  (RGB packed for readability)
color[]  pColor  = {
  color(169,169,169),   // Mercury – grey
  color(230,180, 80),   // Venus   – pale gold
  color( 70,130,200),   // Earth   – blue
  color(210, 90, 50),   // Mars    – red-orange
  color(210,170,110),   // Jupiter – tan
  color(210,195,130),   // Saturn  – warm beige
  color(130,210,210),   // Uranus  – cyan
  color( 60, 80,200)    // Neptune – deep blue
};

float[]  angle   = new float[8];

float moonAngle = 0;

int  hoveredPlanet = -1;
long hoverStart    = 0;

void setup() {
  size(900, 900);          // canvas size
  smooth(8);
  textFont(createFont("Arial", 14, true));

  starX      = new float[NUM_STARS];
  starY      = new float[NUM_STARS];
  starSize   = new float[NUM_STARS];
  starBright = new float[NUM_STARS];
  for (int i = 0; i < NUM_STARS; i++) {
    starX[i]      = random(width);
    starY[i]      = random(height);
    starSize[i]   = random(0.5, 2.5);
    starBright[i] = random(150, 255);
  }

  astAngle  = new float[NUM_ASTEROIDS];
  astRadius = new float[NUM_ASTEROIDS];
  astSize   = new float[NUM_ASTEROIDS];
  for (int i = 0; i < NUM_ASTEROIDS; i++) {
    astAngle[i]  = random(TWO_PI);
    astRadius[i] = random(235, 270);
    astSize[i]   = random(1, 3);
  }

  for (int i = 0; i < 8; i++) angle[i] = random(TWO_PI);
}

void draw() {

  // Advance time
  if (!paused) {
    globalTime += 0.01 * simSpeed;
    moonAngle  += 0.05 * simSpeed;

    // Update each planet's orbital angle
    for (int i = 0; i < 8; i++) {
      angle[i] += (0.01 / period[i]) * simSpeed;
    }

    // Drift asteroids slowly
    for (int i = 0; i < NUM_ASTEROIDS; i++) {
      astAngle[i] += (0.002 / 3.0) * simSpeed;
    }
  }

  background(5, 5, 20);

pushMatrix();
  translate(width / 2 + camX, height / 2 + camY);
  scale(zoom);

  noStroke();
  for (int i = 0; i < NUM_STARS; i++) {
    float tw = starBright[i] + 40 * sin(globalTime * 2 + i);
    fill(tw, tw, tw, tw);
    float sx = starX[i] - width / 2;
    float sy = starY[i] - height / 2;
    ellipse(sx, sy, starSize[i], starSize[i]);
  }

  noFill();
  strokeWeight(0.4);
  for (int i = 0; i < 8; i++) {
    stroke(255, 255, 255, 25);
    ellipse(0, 0, orbitR[i] * 2, orbitR[i] * 2);
  }

  noStroke();
  for (int i = 0; i < NUM_ASTEROIDS; i++) {
    float ax = cos(astAngle[i]) * astRadius[i];
    float ay = sin(astAngle[i]) * astRadius[i];
    fill(130, 120, 110, 180);
    ellipse(ax, ay, astSize[i], astSize[i]);
  }

  drawSun();

  for (int i = 0; i < 8; i++) {
    float px = cos(angle[i]) * orbitR[i];
    float py = sin(angle[i]) * orbitR[i];
    drawPlanet(i, px, py);
  }

  popMatrix();

  drawHUD();
  drawInfoPanel();
}

void drawSun() {
  for (int g = 6; g >= 0; g--) {
    float alpha = map(g, 6, 0, 10, 60);
    float sz    = 36 + g * 14 + 8 * sin(globalTime * 2);
    noStroke();
    fill(255, 200, 50, alpha);
    ellipse(0, 0, sz, sz);
  }
  fill(255, 240, 80);
  ellipse(0, 0, 36, 36);

  strokeWeight(1);
  for (int r = 0; r < 12; r++) {
    float ra = r * PI / 6 + globalTime;
    float len = 20 + 8 * sin(globalTime * 3 + r);
    stroke(255, 220, 100, 80);
    line(cos(ra) * 18, sin(ra) * 18,
         cos(ra) * (18 + len), sin(ra) * (18 + len));
  }
}

void drawPlanet(int i, float px, float py) {
  boolean isHovered = (i == hoveredPlanet);

  if (isHovered) {
    noStroke();
    for (int g = 4; g >= 0; g--) {
      fill(red(pColor[i]), green(pColor[i]), blue(pColor[i]), 20 + g * 10);
      float gs = pRadius[i] * 2 + g * 6;
      ellipse(px, py, gs, gs);
    }
  }

  noStroke();
  fill(pColor[i]);
  ellipse(px, py, pRadius[i] * 2, pRadius[i] * 2);

  fill(0, 0, 20, 90);
  arc(px, py, pRadius[i] * 2, pRadius[i] * 2, PI * 0.5, PI * 1.5);

  if (i == 2) {
    fill(60, 160, 70, 180);
    ellipse(px - 2, py - 1, 7, 5);
    ellipse(px + 3, py + 2, 4, 3);

    float mr = 20;
    float mx = px + cos(moonAngle) * mr;
    float my = py + sin(moonAngle) * mr;
    fill(200, 200, 200);
    ellipse(mx, my, 4, 4);
    noFill();
    stroke(255, 255, 255, 30);
    strokeWeight(0.5);
    ellipse(px, py, mr * 2, mr * 2);
    noStroke();
  }

  if (i == 4) {
    for (int b = -2; b <= 2; b++) {
      stroke(180, 140, 80, 120);
      strokeWeight(1.5);
      float bw = sqrt(max(0, pRadius[i]*pRadius[i] - b*b*9));
      line(px - bw, py + b * 4, px + bw, py + b * 4);
    }
    noStroke();
  }

  if (i == 5) {
    noFill();
    strokeWeight(3);
    stroke(210, 190, 120, 160);
    ellipse(px, py, pRadius[i] * 3.4f, pRadius[i] * 1.0f);
    strokeWeight(1.5);
    stroke(190, 170, 100, 100);
    ellipse(px, py, pRadius[i] * 4.0f, pRadius[i] * 1.2f);
    noStroke();
  }

  if (i == 3) {
    float mr = 14;
    float mx = px + cos(moonAngle * 2) * mr;
    float my = py + sin(moonAngle * 2) * mr;
    fill(180, 160, 140);
    ellipse(mx, my, 2.5f, 2.5f);
  }

  fill(255, 255, 255, isHovered ? 255 : 160);
  textSize(11 / zoom);
  textAlign(CENTER, TOP);
  text(planetName[i], px, py + pRadius[i] + 4 / zoom);
}

void drawHUD() {
  fill(0, 0, 0, 140);
  noStroke();
  rect(0, 0, width, 50);

  fill(255, 220, 80);
  textSize(22);
  textAlign(LEFT, CENTER);
  text("☀  Interactive Solar System", 18, 25);

  fill(200, 200, 200);
  textSize(13);
  textAlign(RIGHT, CENTER);
  text("Speed: " + nf(simSpeed, 1, 1) + "x  |  " +
       (paused ? "PAUSED" : "RUNNING") +
       "  |  Zoom: " + nf(zoom, 1, 2) + "x", width - 18, 25);

  fill(0, 0, 0, 130);
  rect(0, height - 38, width, 38);
  fill(180, 180, 180);
  textSize(12);
  textAlign(CENTER, CENTER);
  text("[SPACE] Pause   [↑↓] Speed   [+/-] Zoom   " +
       "[R] Reset   [Mouse] Hover/Click planet",
       width / 2, height - 19);
}

void drawInfoPanel() {
  if (hoveredPlanet < 0) return;
  int i = hoveredPlanet;

  String[] facts = {
    "Closest planet to the Sun.\nNo atmosphere. Extreme temperatures.",
    "Hottest planet (462 °C avg).\nThick CO₂ atmosphere.",
    "Our home. 71% ocean covered.\nOne natural moon.",
    "The Red Planet.\nLargest volcano: Olympus Mons.",
    "Largest planet.\nGreat Red Spot storm > 300 years old.",
    "Least dense planet.\nIts rings are ice and rock.",
    "Rotates on its side (98° tilt).\n27 known moons.",
    "Strongest winds in the solar system.\n14 known moons."
  };

  String[] stats = {
    "Diameter: 4,879 km  |  Orbit: 88 days",
    "Diameter: 12,104 km |  Orbit: 225 days",
    "Diameter: 12,742 km |  Orbit: 365 days",
    "Diameter: 6,779 km  |  Orbit: 687 days",
    "Diameter: 139,820 km|  Orbit: 11.9 yrs",
    "Diameter: 116,460 km|  Orbit: 29.5 yrs",
    "Diameter: 50,724 km |  Orbit: 84 yrs",
    "Diameter: 49,244 km |  Orbit: 164.8 yrs"
  };

  float pw = 280, ph = 120;
  float px = 18, py = 60;
  fill(10, 15, 40, 210);
  stroke(pColor[i]);
  strokeWeight(1.5);
  rect(px, py, pw, ph, 10);
  noStroke();

  fill(pColor[i]);
  ellipse(px + 22, py + 22, 16, 16);

  fill(255, 240, 100);
  textSize(17);
  textAlign(LEFT, CENTER);
  text(planetName[i], px + 38, py + 22);

  fill(180, 220, 255);
  textSize(11);
  text(stats[i], px + 12, py + 48);

  fill(210, 210, 210);
  textSize(12);
  textLeading(17);
  text(facts[i], px + 12, py + 70);
}

void mouseMoved() {
  float wx = (mouseX - width / 2 - camX) / zoom;
  float wy = (mouseY - height / 2 - camY) / zoom;

  hoveredPlanet = -1;
  for (int i = 0; i < 8; i++) {
    float px = cos(angle[i]) * orbitR[i];
    float py = sin(angle[i]) * orbitR[i];
    float d  = dist(wx, wy, px, py);
    if (d < pRadius[i] + 8) {
      hoveredPlanet = i;
      break;
    }
  }
}

void mousePressed() {
  if (hoveredPlanet >= 0) {
    focusedPlanet = hoveredPlanet;
    zoom = 2.5;
    camX = -cos(angle[focusedPlanet]) * orbitR[focusedPlanet] * zoom;
    camY = -sin(angle[focusedPlanet]) * orbitR[focusedPlanet] * zoom;
  } else {
    focusedPlanet = -1;
    zoom = 1.0;
    camX = 0; camY = 0;
  }
}

void mouseWheel(MouseEvent e) {
  float delta = e.getCount();
  zoom = constrain(zoom - delta * 0.1, 0.3, 5.0);
}

void keyPressed() {
  if (key == ' ')        paused    = !paused;
  if (keyCode == UP)     simSpeed  = constrain(simSpeed + 0.5, 0.1, 10);
  if (keyCode == DOWN)   simSpeed  = constrain(simSpeed - 0.5, 0.1, 10);
  if (key == '+' || key == '=')
                         zoom      = constrain(zoom + 0.1, 0.3, 5.0);
  if (key == '-')        zoom      = constrain(zoom - 0.1, 0.3, 5.0);
  if (key == 'r' || key == 'R') {
    zoom = 1.0; camX = 0; camY = 0;
    simSpeed = 1.0; paused = false;
    focusedPlanet = -1;
    for (int i = 0; i < 8; i++) angle[i] = random(TWO_PI);
  }
}
