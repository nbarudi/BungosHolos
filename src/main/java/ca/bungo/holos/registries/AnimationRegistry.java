package ca.bungo.holos.registries;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.animations.Animation;
import ca.bungo.holos.api.animations.simple.BounceSimpleAnimation;
import ca.bungo.holos.api.animations.simple.HorizontalSimpleAnimation;
import ca.bungo.holos.api.animations.simple.VerticalSimpleAnimation;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AnimationRegistry {

    private final Map<String, Animation> animations;

    public AnimationRegistry() {
        animations = new HashMap<>();

        //Loading base animations
        loadBaseAnimations();
    }

    private void loadBaseAnimations(){
        registerAnimation("bounce", new BounceSimpleAnimation(40, 1f));
        registerAnimation("horizontal", new HorizontalSimpleAnimation(40, 1f));
        registerAnimation("vertical", new VerticalSimpleAnimation(40, 1f));
    }

    /**
     * Register a new animation into the registry.
     * If an animation with the same name already exists, it will be overwritten.
     *
     * @param name The name for the animation
     * @param animation The animation to register.
     */
    public void registerAnimation(String name, Animation animation) {
        animations.put(name, animation);
    }

    /**
     * Retrieve an animation by its name.
     *
     * @param name The name of the animation to retrieve.
     * @return The Animation object if found, or null if it does not exist.
     */
    public Animation getAnimation(String name) {
        return animations.get(name);
    }


    /**
     * Remove an animation from the registry by its name.
     *
     * @param name The name of the animation to remove.
     * @return True if the animation was removed, false if it was not found.
     */
    public boolean removeAnimation(String name) {
        return animations.remove(name) != null;
    }

    /**
     * Clear all animations from the registry.
     * This will remove all registered animations.
     */
    public void clearAnimations() {
        animations.clear();
    }

    /**
     * Get a list of all registered animations' names.
     *
     * @return A set containing the names of all registered animations.
     */
    public Set<String> getRegisteredAnimations() {
        return animations.keySet();
    }

    public CompletableFuture<Animation> waitForAnimation(String animationName) {
        CompletableFuture<Animation> future = new CompletableFuture<>();
        if(!animations.containsKey(animationName)){
            if(!BungosHolos.DISABLED){
                new BukkitRunnable() {
                    public void run() {
                        Animation animation = getAnimation(animationName);
                        if(animation == null) return;
                        future.complete(animation);
                        cancel();
                    }
                }.runTaskTimerAsynchronously(BungosHolos.get(), 10, 10);
            }
            else future.complete(null);
        }
        else future.complete(getAnimation(animationName));
        return future;
    }
}
