/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map.edge;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a single Edge of a MindMap.
 */
public class SummaryEdgeView extends EdgeView {
	private static final int CHILD_XCTRL = 20;
	private static final int XCTRL = 6;

	public SummaryEdgeView(final NodeView source, final NodeView target) {
		super(source, target);
	}

	public SummaryEdgeView(final NodeView target) {
		super(target);
	}

	@Override
	protected void draw(final Graphics2D g) {
		final Shape graph = update();
		final Color color = getColor();
		g.setColor(color);
		final Stroke stroke = getStroke();
		g.setStroke(stroke);
		g.draw(graph);
		if (isTargetEclipsed()) {
			g.setColor(g.getBackground());
			g.setStroke(EdgeView.getEclipsedStroke());
			g.draw(graph);
			g.setStroke(stroke);
			g.setColor(color);
		}
	}

	private Shape update() {
		final int sign = (getTarget().isLeft()) ? -1 : 1;
		final int xctrl = getMap().getZoomed(sign * SummaryEdgeView.XCTRL);
		final int childXctrl = getMap().getZoomed(-1 * sign * SummaryEdgeView.CHILD_XCTRL);
		final GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
		path.moveTo(start.x + xctrl, start.y);
		path.lineTo(start.x + xctrl * 2, start.y);
		path.curveTo(start.x + xctrl * 3, start.y, end.x + childXctrl, end.y, end.x, end.y);
		return path;
	}

	@Override
	public boolean detectCollision(final Point p) {
		final Shape graph = update();
		return new CollisionDetector().detectCollision(p, graph);
	}
}