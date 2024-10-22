package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

public class AccelerationStructure {
    public LongBuffer handle;
    public long deviceAddress;
    public Buffer buffer;

    AccelerationStructure(MemoryStack stack) {
        this.handle = stack.mallocLong(1);
        this.deviceAddress = 0;
        this.buffer = new IndexBuffer(1, MemoryTypes.GPU_MEM, true);
    }
}
