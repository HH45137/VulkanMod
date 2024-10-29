package net.vulkanmod.vulkan.hwrt;

import com.mojang.blaze3d.vertex.MeshData;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;

public class RTGeometry {
    public VertexBuffer vertexBuffer = null;
    public IndexBuffer indexBuffer = null;
    public int numPrimitive = 0;

    RTGeometry(MeshData meshData) {
        this.vertexBuffer = new VertexBuffer(meshData.vertexBuffer().remaining(), MemoryTypes.GPU_MEM, true);
        this.indexBuffer = new IndexBuffer(meshData.drawState().indexCount(), MemoryTypes.GPU_MEM, true);
        this.numPrimitive = meshData.drawState().indexCount() / 3;
    }

    public void free() {
        this.vertexBuffer.freeBuffer();
        this.indexBuffer.freeBuffer();
    }
}
