/*
 * Copyright Samuel Halliday 2010
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
package uk.me.fommil.ff.physics;

import java.util.logging.Logger;
import org.ode4j.drawstuff.DrawStuff;
import org.ode4j.drawstuff.DrawStuff.dsFunctions;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DSphere;
import uk.me.fommil.ff.Main;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.Team;
import uk.me.fommil.ff.swos.TacticsParser;

/**
 * A debugging version of the game using a 3D rendered view.
 *
 * @author Samuel Halliday
 */
public class GamePhysicsGL extends dsFunctions {

	private static final Logger log = Logger.getLogger(GamePhysicsGL.class.getName());

	/** @param args */
	public static final void main(String[] args) throws Exception {
		final int width = 800;
		final int height = 600;
		Team a = new Team();
		a.setCurrentTactics(TacticsParser.getSwosTactics(Main.SWOS).get("442"));
		Pitch pitch = new Pitch();
		GamePhysics game = new GamePhysics(a, pitch);
		GamePhysicsGL demo = new GamePhysicsGL(game);
		DrawStuff.dsSimulationLoop(args, width, height, demo);
	}

	private final GamePhysics game;

	private GamePhysicsGL(GamePhysics game) {
		this.game = game;
	}

	@Override
	public void start() {
	}

	@Override
	public void step(boolean pause) {
		game.tick(0.01);

		Position c = game.getBall().getPosition();

		float[] xyz = {(float) c.x, (float) c.y, 20f};

		float[] hpr = {-90, -90, 0};

		DrawStuff.dsSetViewpoint(xyz, hpr);

		for (Player player : game.getPlayers()) {
			DrawStuff.dsSetColor(1, 0, 0);
			DBox box = (DBox) player.getGeometry();
			DrawStuff.dsDrawBox(box.getPosition(), box.getRotation(), box.getLengths());
		}

		DrawStuff.dsSetColor(1, 1, 1);
		DSphere sphere = (DSphere) game.getBall().getGeometry();
		DrawStuff.dsDrawSphere(sphere.getPosition(), sphere.getRotation(), (float) sphere.getRadius());
	}

	@Override
	public void command(char cmd) {
	}

	@Override
	public void stop() {
	}
}
