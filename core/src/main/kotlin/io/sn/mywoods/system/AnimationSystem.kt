package io.sn.mywoods.system

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import io.sn.mywoods.component.AnimationComponent
import io.sn.mywoods.component.AnimationType
import io.sn.mywoods.component.ImageComponent
import ktx.app.gdxError
import ktx.log.logger

class AnimationSystem(
    private val textureAtlas: TextureAtlas
) : IteratingSystem(World.family {
    all(AnimationComponent, ImageComponent)
}) {
    companion object {
        private val log = logger<AnimationSystem>()

        fun nextRotation(aniType: String): String {
            return when (aniType.ifEmpty { "up" }.split("/")[0]) {
                "up" -> "left"
                "left" -> "right"
                "right" -> "down"
                "down" -> "up"
                else -> ""
            }.let {
                AnimationType.valueOf(it.uppercase()).atlasKey
            }
        }
    }

    private val cache = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    override fun onTickEntity(entity: Entity) {
        val etyAniCmp = entity[AnimationComponent]
        val etyImgCmp = entity[ImageComponent]

        val frame = when {
            etyAniCmp.nextAnimation.startsWith("#") -> {
                etyAniCmp.animation = animation(0f, etyAniCmp.nextAnimation.slice(1 until etyAniCmp.nextAnimation.length))
                etyAniCmp.stateTime = 0f
                etyAniCmp.animation.getKeyFrame(0f)
            }

            else -> {
                etyAniCmp.animation = animation(etyAniCmp.frameDuration, etyAniCmp.nextAnimation)
                etyAniCmp.stateTime += deltaTime
                etyAniCmp.animation.playMode = etyAniCmp.playMode
                etyAniCmp.animation.getKeyFrame(etyAniCmp.stateTime)
            }
        }

        etyImgCmp.image.drawable = frame
    }

    private fun animation(frameDuration: Float, aniKeyPath: String): Animation<TextureRegionDrawable> = cache.getOrPut(aniKeyPath) {
        log.debug { "Creation of new animation for $aniKeyPath" }
        val regions = textureAtlas.findRegions(aniKeyPath)
        if (regions.isEmpty) {
            gdxError("Cannot find any region for $aniKeyPath")
        }
        Animation(frameDuration, *regions.map {
            TextureRegionDrawable(it)
        }.toTypedArray())
    }

}