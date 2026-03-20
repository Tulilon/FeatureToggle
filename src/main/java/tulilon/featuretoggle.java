package tulilon;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jspecify.annotations.NonNull;
import tulilon.mixin.FeatureFlagRegistryAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class featuretoggle implements ModInitializer {
	public static Map<Identifier, Boolean> changeOnStop = new HashMap<>();
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(getToggleFeatureCommand());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onStop);
	}
	private void onStop(MinecraftServer server) {

		List<Identifier> enable = new ArrayList<>();
		List<Identifier> disable = new ArrayList<>();

		for (Identifier flag : changeOnStop.keySet()) {
			if (changeOnStop.get(flag)) enable.add(flag);
			else if (!changeOnStop.get(flag)) disable.add(flag);
		}

		FeatureFlagSet enableSet = toFeatureFlagSet(enable);
		FeatureFlagSet disableSet = toFeatureFlagSet(disable);
		FeatureFlagSet newFeatures = server.getWorldData().enabledFeatures();

		newFeatures = newFeatures.join(enableSet);
		newFeatures = newFeatures.subtract(disableSet);

		WorldDataConfiguration serverDataConfig = server.getWorldData().getDataConfiguration();
		WorldDataConfiguration newConfig = new WorldDataConfiguration(serverDataConfig.dataPacks(), newFeatures);

		server.getWorldData().setDataConfiguration(newConfig);
	}

	private static @NonNull FeatureFlagSet toFeatureFlagSet(List<Identifier> flags) {
		return FeatureFlags.REGISTRY.fromNames(flags);
	}
	private LiteralArgumentBuilder<CommandSourceStack> getToggleFeatureCommand() {
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("togglefeature");
		for (Identifier id : ((FeatureFlagRegistryAccessor) FeatureFlags.REGISTRY).getNames().keySet()) {

			if (id.getPath().equals("vanilla")) continue;

			command.then(Commands.literal(id.getPath())
					.then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
						boolean value = BoolArgumentType.getBool(context, "value");
						changeOnStop.put(id, value);
						context.getSource().sendSuccess(() ->getSuccessMessage(id,value),true);
						return 1;
					})));
		}
		return command;
	}


	private Component getSuccessMessage(Identifier featureID,boolean value) {
		return Component.literal("Set feature ")
				.append(Component.literal(featureID.toString()).withStyle(ChatFormatting.AQUA))
				.append(" to ")
				.append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN))
				.append(Component.literal("\nYou will need to restart the world/server to apply the changes").withStyle(ChatFormatting.GRAY));
	}
}