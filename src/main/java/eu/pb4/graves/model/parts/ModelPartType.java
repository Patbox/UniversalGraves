package eu.pb4.graves.model.parts;

public enum ModelPartType {
    BLOCK(BlockDisplayModelPart.class),
    ITEM(ItemDisplayModelPart.class),
    TEXT(TextDisplayModelPart.class),
    ENTITY(EntityModelPart.class),
    PARTICLE(ParticleModelPart.class)
    ;

    public final Class<?> modelPartClass;

    <T extends ModelPart<?, ?>> ModelPartType(Class<T> modelPartClass) {
        this.modelPartClass = modelPartClass;
    }
}
