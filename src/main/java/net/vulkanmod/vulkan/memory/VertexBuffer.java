package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

public class VertexBuffer extends Buffer {

    public VertexBuffer(int size) {
        this(size, MemoryTypes.HOST_MEM);
    }

    public VertexBuffer(int size, MemoryType type) {
        super(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, type);
        this.createBuffer(size);

    }

    public VertexBuffer(int size, MemoryType type, boolean rtGeometry) {
        super(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, type);

        if (rtGeometry) {
            this.usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        }

        this.createBuffer(size);
    }

    public void copyToVertexBuffer(long vertexSize, long vertexCount, ByteBuffer byteBuffer) {
        int bufferSize = (int) (vertexSize * vertexCount);
//        long bufferSize = byteBuffer.limit();

        if(bufferSize > this.bufferSize - this.usedBytes) {
            resizeBuffer((this.bufferSize + bufferSize) * 2);
        }

        this.type.copyToBuffer(this, bufferSize, byteBuffer);
        offset = usedBytes;
        usedBytes += bufferSize;

    }

    private void resizeBuffer(int newSize) {
        MemoryManager.getInstance().addToFreeable(this);
        this.createBuffer(newSize);

//        System.out.println("resized vertexBuffer to: " + newSize);
    }

}
