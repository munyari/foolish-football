/*
 * Copyright Samuel Halliday 2009
 * 
 * This file is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this file.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package uk.me.fommil.ff;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import static java.lang.Math.*;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The model (M) and controller (C) for a {@link Player} during game play.
 *
 * @author Samuel Halliday
 */
public class PlayerMC {

	/**
	 * The actions that a player can perform.
	 */
	public enum Action {

		UP, DOWN, LEFT, RIGHT, KICK, TACKLE, HEAD

	};

	public enum PlayerState {

		RUN, KICK, TACKLE, HEAD_START, HEAD_MID, HEAD_END, GROUND, INJURED,
		THROW,
		// TODO CELEBRATE, PUNISH
	}

	private static final Logger log = Logger.getLogger(PlayerMC.class.getName());

	protected static final int AUTO = 10;

	protected static final double TACKLE_FRICTION = 50;

	protected static final double HEADING_FRICTION = 20;
	
	protected final Player player;

	protected final int shirt;

	protected final Point3d s = new Point3d();

	protected final Vector3d v = new Vector3d();

	protected final Vector3d facing = new Vector3d(0, -1, 0);

	protected volatile PlayerState mode = PlayerState.RUN;

	protected volatile double time;

	private volatile double timestamp = Double.NaN; // of last mode switch

	protected final Random random = new Random();

	/**
	 * @param i
	 * @param player
	 */
	public PlayerMC(int i, Player player) {
		Preconditions.checkArgument(i >= 1 && i <= 11, i);
		Preconditions.checkNotNull(player);
		this.shirt = i;
		this.player = player;
	}

	/**
	 * Return the volume in which this player can control the ball.
	 *
	 * @return
	 */
	public BoundingPolytope getBounds() {
		BoundingPolytope b = new BoundingPolytope();

		// TODO: heading and tackling bounds (should follow same pattern)

		Transform3D affine = new Transform3D();
		Vector3d t = new Vector3d(facing);
		// centre of bounding box is biased in front of the player
		//t.scale(1);
		t.add(s);
		affine.setTranslation(t);
		// defines the scale of the bounding box in x, y, z
		// TODO: control could determine the x scale
		affine.setScale(new Vector3d(8, 5, 2));
		// rotate
		affine.setRotation(new AxisAngle4d(0, 0, 1, getAngle()));

		b.transform(affine);
		return b;
	}

	/**
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		time += t;
//		log.info(s + " " + v);
		assert mode != null;
		switch (mode) {
			case GROUND:
			case THROW:
			case INJURED:
				//v.scale(0);
				break;
			default:
				Vector3d dv = (Vector3d) v.clone();
				dv.scale(t);
				s.add(dv);
		}

		switch (mode) {
			case KICK:
				changeModeIfTimeExpired(0.1, PlayerState.RUN);
				break;
			case TACKLE:
				v.x = signum(v.x) * max(0, abs(v.x) - t * TACKLE_FRICTION);
				v.y = signum(v.y) * max(0, abs(v.y) - t * TACKLE_FRICTION);
				changeModeIfTimeExpired(3, PlayerState.RUN);
				break;
			case HEAD_START:
			case HEAD_MID:
			case HEAD_END:
				v.x = signum(v.x) * max(0, abs(v.x) - t * HEADING_FRICTION);
				v.y = signum(v.y) * max(0, abs(v.y) - t * HEADING_FRICTION);
				if (mode == PlayerState.HEAD_START)
					changeModeIfTimeExpired(0.1, PlayerState.HEAD_MID);
				else if (mode == PlayerState.HEAD_MID)
					changeModeIfTimeExpired(0.1, PlayerState.HEAD_END);
				else if (mode == PlayerState.HEAD_END)
					changeModeIfTimeExpired(0.5, PlayerState.GROUND);
				break;
			case GROUND:
				if (random.nextBoolean())
					changeModeIfTimeExpired(2, PlayerState.RUN);
				else
					changeModeIfTimeExpired(2, PlayerState.INJURED);
				break;
			case INJURED:
				changeModeIfTimeExpired(5, PlayerState.RUN);
		}
	}

	/**
	 * Controller.
	 * 
	 * @param actions
	 */
	public void setActions(Collection<Action> actions) {
//		log.info(shirt + " " + mode + " " + actions);
		switch (mode) {
			case RUN:
			case THROW:
				break;
			default:
				return;
		}
		assert Double.isNaN(timestamp) : mode;
		if (actions.contains(Action.KICK)) {
			ifMovingChangeModeAndScaleVelocity(PlayerState.KICK, 1);
			return;
		} else if (actions.contains(Action.TACKLE)) {
			ifMovingChangeModeAndScaleVelocity(PlayerState.TACKLE, 1.5);
			return;
		} else if (actions.contains(Action.HEAD)) {
			ifMovingChangeModeAndScaleVelocity(PlayerState.HEAD_START, 1.5);
			return;
		}

		Vector3d newV = new Vector3d();
		for (Action action : actions) {
			switch (action) {
				case UP:
					newV.y -= 1;
					break;
				case DOWN:
					newV.y += 1;
					break;
				case LEFT:
					newV.x -= 1;
					break;
				case RIGHT:
					newV.x += 1;
					break;
			}
		}
		if (newV.lengthSquared() > 0) {
			newV.normalize(); // TODO: is there a more efficient way?
			newV.scale(50);
		}
		v.set(newV);
		if (v.lengthSquared() > 0) {
			facing.normalize(v);
		}
	}

	/**
	 * Controller. Clear the action list.
	 */
	public void clearActions() {
		v.scale(0);
	}

	/**
	 * Controller. Ignore user input and go to the zone indicated.
	 *
	 * @param attractor
	 */
	public void autoPilot(Point3d attractor) {
		Preconditions.checkNotNull(attractor);
		List<PlayerMC.Action> auto = Lists.newArrayList();
		double dx = s.x - attractor.x;
		if (dx < -AUTO) {
			auto.add(PlayerMC.Action.RIGHT);
		} else if (dx > AUTO) {
			auto.add(PlayerMC.Action.LEFT);
		}
		double dy = s.y - attractor.y;
		if (dy < -AUTO) {
			auto.add(PlayerMC.Action.DOWN);
		} else if (dy > AUTO) {
			auto.add(PlayerMC.Action.UP);
		}
		setActions(auto);
	}

	/**
	 * @return the angle relate to NORTH {@code (- PI, + PI]}.
	 */
	public double getAngle() {
		return Utils.getBearing(facing);
	}

	@Override
	public String toString() {
		return shirt + ", mode = " + mode + ", s = " + s + ", v = " + v;
	}

	protected void ifMovingChangeModeAndScaleVelocity(PlayerState playerMode, double scale) {
		if (v.lengthSquared() > 0) {
			timestamp = time;
			mode = playerMode;
			v.scale(scale);
		}
	}

	protected void changeModeIfTimeExpired(double t, PlayerState playerMode) {
		if (time - timestamp > t) {
			mode = playerMode;
			if (playerMode == PlayerState.RUN)
				timestamp = Double.NaN;
			else
				timestamp = time;
		}
	}

	public void setThrowIn() {
		mode = PlayerState.THROW;
		timestamp = Double.NaN;
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public int getShirt() {
		return shirt;
	}

	public PlayerState getMode() {
		return mode;
	}

	public Point3d getPosition() {
		return (Point3d) s.clone();
	}

	public void setPosition(Point3d s) {
		Preconditions.checkNotNull(s);
		this.s.set(s);
	}

	public Vector3d getVelocity() {
		return (Vector3d) v.clone();
	}
	// </editor-fold>
}
