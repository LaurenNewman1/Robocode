package edu.ufl.cise.cs1.robots;

import robocode.TeamRobot;
import robocode.*;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import java.awt.geom.Point2D;
import java.awt.*;

public class Phoebee extends TeamRobot
{
    // Declare all variables
    private int direction = 1;
    private double prevEnergy;
    private double enemyBearing;
    private double nonNormalBearing;
    private double normalBearing;
    private double normalHeading;
    private double radarBearing;
    private double enemyX;
    private double enemyY;
    private double enemyDirection;
    private double enemyDistanceMoved;
    private double enemySlope;
    private double prevEnemyX;
    private double prevEnemyY;
    private double shootingX;
    private double shootingY;
    private double shootingAngle;
    private boolean bulletHit;

    public void run()
    {
        // Set colors
        setBodyColor(Color.black);
        setGunColor(Color.yellow);
        setRadarColor(Color.yellow);
        setBulletColor(Color.yellow);
        setScanColor(Color.yellow);

        // Other settings
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Radar scans infinitely
        setAdjustGunForRobotTurn(true);
        do {
            if ( getRadarTurnRemaining() == 0.0 )
                setTurnRadarRight( Double.POSITIVE_INFINITY );
            execute();
            scan();
        } while ( true );
    }

    // When radar detects a robot within 1200 pixels
    public void onScannedRobot(ScannedRobotEvent e)
    {
        // Lock radar on the enemy
        double enemyAngle = getHeading() + e.getBearing();
        double radarAngle = Utils.normalRelativeAngleDegrees(enemyAngle - getRadarHeading());
        double extraAngle = Math.min( Math.atan( 36.0 / e.getDistance() ), Rules.RADAR_TURN_RATE);
        if (radarAngle < 0)
            radarAngle -= extraAngle;
        else
            radarAngle += extraAngle;
        setTurnRadarRight(2.0 * radarAngle);

        // Declare variables
        enemyBearing = e.getBearing();
        prevEnemyX = enemyX;
        prevEnemyY = enemyY;
        enemyX = (getX() + Math.sin(getHeading() - getGunHeading() + e.getBearing()) * e.getDistance());
        enemyY = (getY() + Math.cos(getHeading() - getGunHeading() + e.getBearing()) * e.getDistance());
        enemyDirection = absoluteBearing(prevEnemyX, prevEnemyY, enemyX, enemyY);
        enemyDistanceMoved = getDistance(prevEnemyX, prevEnemyY, enemyX, enemyY);
        enemySlope = getSlope(prevEnemyX, prevEnemyY, enemyX, enemyY);
        shootingX = getNewX(enemyX, prevEnemyX, enemyDistanceMoved);
        shootingY = getNewY(enemyY, enemySlope, enemyDistanceMoved);
        shootingAngle = absoluteBearing(getX(), getY(), shootingX, shootingY);


        // If stuck in corner, ram enemy
        if (Math.abs(getBattleFieldHeight() - getY()) < 100 && Math.abs(getBattleFieldWidth() - getX()) < 100)
        {
            setTurnRight(e.getBearing());
            ahead(e.getDistance() + 5);
        }

        // Normalize bearing and heading
        nonNormalBearing = getHeading() - getGunHeading() + e.getBearing();
        normalBearing = normalizeAngle(nonNormalBearing);
        normalHeading = normalizeAngle(e.getHeading());

        // If enemy has fired a bullet, dodge
        if (prevEnergy - e.getEnergy() > 0.1 && e.getEnergy() - prevEnergy < 3.0
            && bulletHit == false)
        {
            setTurnRight(e.getBearing() + 90 - (15 * direction));
            ahead((e.getDistance() / 4 + 25) * direction);
            setTurnGunRight(normalBearing);
            // Only fire if gun has rotated fully
            if (getEnergy() > 10 && getGunTurnRemaining() < 15 && getGunTurnRemaining() > -15)
            {
                fire(getFirepower(e.getEnergy(), getEnergy(), e.getDistance()));
            }
            direction *= -1;
        }

        setTurnGunRight(normalBearing);

        // Only fire if gun has rotated fully
        if (getGunTurnRemaining() < 15 && getGunTurnRemaining() > -15)
        {
            fire(getFirepower(e.getEnergy(), getEnergy(), e.getDistance()));
        }

        // If the enemy is close, ram them
        if (e.getDistance() < 100 && normalHeading - normalBearing > 10 && normalHeading - normalBearing < -10)
        {
            setTurnRight(e.getBearing());
            ahead(e.getDistance() + 5);
        }

        // Store previous energy for bullet detection
        prevEnergy = e.getEnergy();
    }

    // When hit by a bullet
    public void onHitByBullet(HitByBulletEvent e)
    {
        // If enemy is straight ahead, fire back
        if (e.getBearing() < 10 || e.getBearing() > - 10)
        {
            nonNormalBearing = getHeading() - getGunHeading() + e.getBearing();
            normalBearing = normalizeAngle(nonNormalBearing);
            setTurnGunRight(normalBearing);
            fire(1.1);
        }

        // Move away from the enemy at a 90 degree angle
        setTurnRight(e.getBearing() + 90);
        ahead(100 * direction);
    }

    // When hits a wall
    public void onHitWall(HitWallEvent e)
    {
        // Move directly away from the wall
        direction *= -1;
        setTurnRight(e.getBearing());
        ahead(200 * direction);
    }

    // When hits a robot
    public void onHitRobot(HitRobotEvent e)
    {
        // Move directly away from the robot
        direction *= 1;
        ahead(250 * direction);
    }

    // When a bullet successfully hits the enemy
    public void onBulletHit(BulletHitEvent e)
    {
        bulletHit = true;
    }

    // When a bullet misses
    public void onBulletMissed(BulletMissedEvent e)
    {
        bulletHit = false;
    }

    // My Method #1
    public static double normalizeAngle(double angle)
    {
        /* Code snippet credit to Mark Whitley
        http://mark.random-article.com/weber/java/robocode/lesson4.html
         */
        while (angle >  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // My Method #2
    // computes the absolute bearing between two points
    /* Code snippet credit to Mark Whitley
        http://mark.random-article.com/weber/java/robocode/lesson4.html
         */
    double absoluteBearing(double myX, double myY, double enemyX, double enemyY) {
        double xo = enemyX-myX;
        double yo = enemyY-myY;
        double hyp = Point2D.distance(myX, myY, enemyX, enemyY);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }
        return bearing;
    }

    // My Method #3
    public static double angleToShoot(double normalHeading, double normalBearing, double enemyVelocity)
    {
        if (enemyVelocity > 1)
        {
            if (normalHeading - normalBearing > 45)
            {
                return normalBearing + 5;
            }
            else if (normalHeading - normalBearing < -45)
            {
                return normalBearing - 5;
            }
        }
        return normalBearing;
    }

    // My Method #4
    public static double getFirepower(double enemyEnergy, double myEnergy, double distance)
    {
        if (enemyEnergy < 1 && myEnergy > 3)
            return 3;
        else if (enemyEnergy < 1 && myEnergy < 3)
            return myEnergy - 0.1;
        else if (distance < 200 && myEnergy > 20)
            return 3;
        else
            return 1.1;
    }

    // My Method #5
    public static double getDistance(double firstX, double firstY, double secondX, double secondY)
    {
        double distance;
        distance = Math.sqrt(Math.pow(secondY - firstY, 2) + Math.pow(secondX - firstX, 2));
        return distance;
    }

    // My Method #6
    public static double getSlope(double firstX, double firstY, double secondX, double secondY)
    {
        if(firstX == secondX)
        {
            return 0;
        }

        double slope;
        slope = (secondY - firstY) / (secondX - firstX);
        return slope;
    }

    // My Method #7
    public static double getNewX(double currentX, double prevX, double distance)
    {
        double newX;
        if (currentX > prevX)
        {
            newX = currentX + 10;
        }
        else
        {
            newX = currentX - 10;
        }
        return newX;
    }

    // My Method #8
    public static double getNewY(double currentY, double slope, double distance)
    {
        if(slope == 0)
        {
            return currentY;
        }

        double newY;
        newY = currentY + (10 * slope);
        return newY;
    }
}


