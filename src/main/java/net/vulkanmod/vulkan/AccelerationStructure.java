package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;

import java.nio.LongBuffer;

public class AccelerationStructure {
    public long AS;
    public Buffer buffer;
    public boolean isCreated;

    public AccelerationStructure() {
        this.isCreated = false;
        this.AS = 0;
        this.buffer = new IndexBuffer(1, MemoryTypes.GPU_MEM, true);
    }
}
