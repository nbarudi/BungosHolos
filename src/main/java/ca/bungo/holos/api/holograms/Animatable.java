package ca.bungo.holos.api.holograms;

import ca.bungo.holos.api.animations.Animation;

public interface Animatable {

    /**
     * Load the animation the Hologram will be playing
     * @param animation Animation to play
     * */
    void loadAnimation(Animation animation);

    /**
     * Get the currently loaded animation
     * @return Current Animation
     * */
    Animation getAnimation();

    /**
     * Start playing the animation
     * */
    void playAnimation();

    /**
     * Stop and reset the animation
     * */
    void stopAnimation();

    /**
     * Pause and Unpause the currently playing animation
     * <p>
     * Must have {@link #playAnimation()} triggered first
     * */
    void toggleAnimation();

    /**
     * Get data on if an animation is currently playing
     * @return Is there an animation playing
     * */
    boolean isPlaying();


}
