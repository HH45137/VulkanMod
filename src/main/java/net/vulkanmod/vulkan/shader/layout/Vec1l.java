package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1l extends Uniform {
    private Supplier<Long> longSupplier;

    public Vec1l(Info info) {
        super(info);
    }

    void setSupplier() {
        this.longSupplier = Uniforms.ac_uniformMap.get(this.info.name);
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.longSupplier = () -> supplier.get().buffer.getLong(0);
    }

    void update(long ptr) {
        Long i = this.longSupplier.get();
        MemoryUtil.memPutLong(ptr + this.offset, i);
    }
}
