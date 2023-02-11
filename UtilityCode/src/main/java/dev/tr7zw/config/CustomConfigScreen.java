package dev.tr7zw.config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CycleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.TooltipSupplier;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.SliderButton;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

public abstract class CustomConfigScreen extends Screen {

    protected final Screen lastScreen;
    private OptionsList list;

    public CustomConfigScreen(Screen lastScreen, String title) {
        super(new TranslatableComponent(title));
        this.lastScreen = lastScreen;
    }

    @Override
    public void removed() {
        save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    public OptionsList getOptions() {
        return list;
    }

    protected void init() {
        this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.addWidget(this.list);
        this.createFooter();
        initialize();
    }

    public abstract void initialize();

    public abstract void reset();

    public abstract void save();

    protected void createFooter() {
        this.addRenderableWidget(
                new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, new OnPress() {

                    @Override
                    public void onPress(Button button) {
                        CustomConfigScreen.this.onClose();
                    }
                }));
        this.addRenderableWidget(new Button(this.width / 2 + 110, this.height - 27, 60, 20,
                new TranslatableComponent("controls.reset"), new OnPress() {

                    @Override
                    public void onPress(Button button) {
                        reset();
                        CustomConfigScreen.this.resize(minecraft, width, height); // refresh
                    }
                }));
    }

    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        this.list.render(poseStack, i, j, f);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 16777215);
        super.render(poseStack, i, j, f);
        List<FormattedCharSequence> list = OptionsSubScreen.tooltipAt(this.list, i, j);
        this.renderTooltip(poseStack, list, i, j);
    }

    @SuppressWarnings("resource")
    private void updateText(ProgressOption option) {
        AbstractWidget widget = getOptions().findOption(option);
        if (widget instanceof SliderButton) {
            ((SliderButton) widget).setMessage(option.getMessage(Minecraft.getInstance().options));
        } else {
            System.out.println(widget.getClass().getName());
        }
    }

    private List<FormattedCharSequence> getTooltip(String translationKey) {
        String key = translationKey + ".tooltip";
        Component comp = new TranslatableComponent(key);
        if(key.equals(comp.getString())) {
            return null;
        } else {
            return minecraft.font.split(comp, 170);
        }
    }

    @SuppressWarnings("unchecked")
    public CycleOption<Boolean> getBooleanOption(String translationKey, Supplier<Boolean> current,
            Consumer<Boolean> update) {
        return CycleOption.createBinaryOption(translationKey, new TranslatableComponent(translationKey + ".on"),
                new TranslatableComponent(translationKey + ".off"), options -> current.get(),
                (options, option, boolean_) -> update.accept(boolean_)).setTooltip((mc) -> (CycleButton.TooltipSupplier<Boolean>)(Object)createStaticTooltip(translationKey).apply(mc));
    }

    @SuppressWarnings("unchecked")
    public CycleOption<Boolean> getOnOffOption(String translationKey, Supplier<Boolean> current,
            Consumer<Boolean> update) {
        return CycleOption.createOnOff(translationKey, options -> current.get(),
                (options, option, boolean_) -> update.accept(boolean_)).setTooltip((mc) -> (CycleButton.TooltipSupplier<Boolean>)(Object)createStaticTooltip(translationKey).apply(mc));
    }

    public ProgressOption getDoubleOption(String translationKey, float min, float max, float steps,
            Supplier<Double> current, Consumer<Double> update) {
        TranslatableComponent comp = new TranslatableComponent(translationKey);
        AtomicReference<ProgressOption> option = new AtomicReference<>();
        option.set(new ProgressOption(translationKey, min, max, steps, (options) -> current.get(), (options, val) -> {
            update.accept(val);
            updateText(option.get());
        }, (options, opt) -> comp.copy().append(": " + round(current.get(), 3)), (mc) -> getTooltip(translationKey)));
        return option.get();
    }

    public ProgressOption getIntOption(String translationKey, float min, float max, Supplier<Integer> current,
            Consumer<Integer> update) {
        TranslatableComponent comp = new TranslatableComponent(translationKey);
        AtomicReference<ProgressOption> option = new AtomicReference<>();
        option.set(
                new ProgressOption(translationKey, min, max, 1, (options) -> (double) current.get(), (options, val) -> {
                    update.accept(val.intValue());
                    updateText(option.get());
                }, (options, opt) -> comp.copy().append(": " + current.get()), (mc) -> getTooltip(translationKey)));
        return option.get();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T extends Enum> CycleOption getEnumOption(String translationKey, Class<T> targetEnum, Supplier<T> current,
            Consumer<T> update) {
        return CycleOption.create(translationKey, Arrays.asList(targetEnum.getEnumConstants()),
                (t) -> new TranslatableComponent(translationKey + "." + t.name()), options -> current.get(),
                (options, option, value) -> update.accept(value)).setTooltip((mc) -> (CycleButton.TooltipSupplier<T>)(Object)createStaticTooltip(translationKey).apply(mc));
    }

    public <T> Function<Minecraft, CycleButton.TooltipSupplier<T>> createStaticTooltip(String translationKey) {
        return (minecraft) -> {
            return new TooltipSupplier<T>() {

                @Override
                public List<FormattedCharSequence> apply(T t) {
                    String key = translationKey + ".tooltip";
                    Component comp = new TranslatableComponent(key);
                    if(key.equals(comp.getString())) {
                        return null;
                    } else {
                        return minecraft.font.split(comp, 170);
                    }
                }
            };
        };
    }

    public static double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}