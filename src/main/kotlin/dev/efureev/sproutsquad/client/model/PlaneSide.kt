package dev.efureev.sproutsquad.client.model

import net.minecraft.core.Direction
import java.util.EnumSet

/**
 * One visible side for a flat (zero-thickness) box: `addBox(x, y, z, w, h, d, PlaneSide.TOP)`.
 *
 * A flat box gets both of its big faces in the same plane, and the entity render type (`entity_cutout`)
 * does not cull back faces, so both are drawn and fight over the depth buffer: where both texture squares
 * are opaque the plane flickers between them as the model moves. One face, drawn from both sides, does not.
 *
 * Model Y points down, so the side seen from above is [Direction.DOWN] (the plane's first UV square).
 * Keep the side the plane is usually seen from: the top of a leaf, the front of an eyebrow.
 */
object PlaneSide {
    /** A horizontal plane seen from above: its first UV square. */
    @JvmField val TOP: Set<Direction> = EnumSet.of(Direction.DOWN)
    /** A horizontal plane seen from below: its second UV square. */
    @JvmField val BOTTOM: Set<Direction> = EnumSet.of(Direction.UP)
    /** A vertical plane (depth 0) on the model's front side, where its face is. */
    @JvmField val NORTH: Set<Direction> = EnumSet.of(Direction.NORTH)
    @JvmField val SOUTH: Set<Direction> = EnumSet.of(Direction.SOUTH)
    /** A vertical plane of width 0; on a mirrored box the two swap sides. */
    @JvmField val WEST: Set<Direction> = EnumSet.of(Direction.WEST)
    @JvmField val EAST: Set<Direction> = EnumSet.of(Direction.EAST)
}
