package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.memory.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.vkGetBufferDeviceAddressKHR;
import static org.lwjgl.vulkan.VK12.*;

public class RayTracing {

    public static boolean RAY_TRACING = true;

    public static AccelerationStructure BLAS = null;

    public static long getBufferDeviceAddress(Buffer buffer) {
        long address;
        try (MemoryStack stack = stackPush()) {
            address = vkGetBufferDeviceAddressKHR(getVkDevice(), VkBufferDeviceAddressInfo
                    .calloc(stack)
                    .sType$Default()
                    .buffer(buffer.getId()));
        }
        return address;
    }

    public static long getBufferDeviceAddress(long buffer) {
        long address;
        try (MemoryStack stack = stackPush()) {
            address = vkGetBufferDeviceAddressKHR(getVkDevice(), VkBufferDeviceAddressInfo
                    .calloc(stack)
                    .sType$Default()
                    .buffer(buffer));
        }
        return address;
    }

    public static VkDeviceOrHostAddressKHR getDeviceAddress(MemoryStack stack, long buffer) {
        return VkDeviceOrHostAddressKHR
                .malloc(stack)
                .deviceAddress(getBufferDeviceAddress(buffer));
    }

    public static VkCommandBuffer createCommandBuffer(long pool, int beginFlags) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pCB = stack.mallocPointer(1);
            vkAllocateCommandBuffers(
                    getVkDevice(),
                    VkCommandBufferAllocateInfo
                            .calloc(stack)
                            .sType$Default()
                            .commandPool(pool)
                            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                            .commandBufferCount(1),
                    pCB);

            VkCommandBuffer cB = new VkCommandBuffer(pCB.get(0), getVkDevice());
            vkBeginCommandBuffer(cB,
                    VkCommandBufferBeginInfo
                            .calloc(stack)
                            .sType$Default()
                            .flags(beginFlags));

            return cB;
        }
    }

    public static PointerBuffer pointersOfElements(MemoryStack stack, CustomBuffer<?> buffer) {
        int remaining = buffer.remaining();
        long addr = buffer.address();
        long sizeof = buffer.sizeof();
        PointerBuffer pointerBuffer = stack.mallocPointer(remaining);
        for (int i = 0; i < remaining; i++) {
            pointerBuffer.put(i, addr + sizeof * i);
        }
        return pointerBuffer;
    }

    public static void setBLAS(MeshData meshData) {
        if (BLAS == null) {
            BLAS = new AccelerationStructure();
            try (MemoryStack stack = stackPush()) {
                RTGeometry rtGeometry = new RTGeometry(meshData);

                MeshData.DrawState DS = meshData.drawState();

                int numTriangles = DS.indexCount() / 3;
                int numVertices = DS.indexCount();
                int vertexStride = CustomVertexFormat.COMPRESSED_TERRAIN.getVertexSize();
                int vertexFormat = getVertexFormat(DS);
                int indexType = VK_INDEX_TYPE_UINT16;

                VkDeviceOrHostAddressConstKHR vertexBufferDeviceAddress = VkDeviceOrHostAddressConstKHR.malloc(stack);
                VkDeviceOrHostAddressConstKHR indexBufferDeviceAddress = VkDeviceOrHostAddressConstKHR.malloc(stack);

                vertexBufferDeviceAddress.deviceAddress(getBufferDeviceAddress(rtGeometry.vertexBuffer));
                indexBufferDeviceAddress.deviceAddress(getBufferDeviceAddress(rtGeometry.indexBuffer));

                // Build and get size info
                VkAccelerationStructureBuildGeometryInfoKHR.Buffer accelerationStructureBuildGeometryInfo =
                        VkAccelerationStructureBuildGeometryInfoKHR
                                .calloc(1, stack)
                                .sType$Default()
                                .type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
                                .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
                                .geometryCount(1)
                                .pGeometries(VkAccelerationStructureGeometryKHR
                                        .calloc(1, stack)
                                        .sType$Default()
                                        .geometryType(VK_GEOMETRY_TYPE_TRIANGLES_KHR)
                                        .geometry(VkAccelerationStructureGeometryDataKHR
                                                .calloc(stack)
                                                .triangles(VkAccelerationStructureGeometryTrianglesDataKHR
                                                        .calloc(stack)
                                                        .sType$Default()
                                                        .vertexFormat(vertexFormat)
                                                        .vertexData(vertexBufferDeviceAddress)
                                                        .vertexStride(vertexStride)
                                                        .maxVertex(numVertices)
                                                        .indexType(indexType)
                                                        .indexData(indexBufferDeviceAddress)))
                                        .flags(VK_GEOMETRY_OPAQUE_BIT_KHR));

                VkAccelerationStructureBuildSizesInfoKHR accelerationStructureBuildSizesInfo = VkAccelerationStructureBuildSizesInfoKHR
                        .malloc(stack)
                        .sType$Default()
                        .pNext(0);
                vkGetAccelerationStructureBuildSizesKHR(
                        getVkDevice(),
                        VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,
                        accelerationStructureBuildGeometryInfo.get(0),
                        stack.ints(1),
                        accelerationStructureBuildSizesInfo
                );

                // Create buffer and memory
                MemoryManager.getInstance().createBuffer(
                        BLAS.buffer,
                        Math.toIntExact(accelerationStructureBuildSizesInfo.accelerationStructureSize()),
                        VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR,
                        0
                );

                // Acceleration Structure
                VkAccelerationStructureCreateInfoKHR accelerationStructureCreateInfo = VkAccelerationStructureCreateInfoKHR.calloc(stack);
                accelerationStructureCreateInfo.sType$Default();
                accelerationStructureCreateInfo.sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR);
                accelerationStructureCreateInfo.buffer(BLAS.buffer.getId());
                accelerationStructureCreateInfo.size(accelerationStructureBuildSizesInfo.accelerationStructureSize());
                accelerationStructureCreateInfo.type(VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR);
                BLAS.handle = stack.mallocLong(1);
                vkCreateAccelerationStructureKHR(getVkDevice(), accelerationStructureCreateInfo, null, BLAS.handle);

                LongBuffer scratchBuffer = stack.mallocLong(1);
                PointerBuffer pStagingAllocation = stack.pointers(0L);
                MemoryManager.getInstance().createBuffer(
                        accelerationStructureBuildSizesInfo.buildScratchSize(),
                        VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT_KHR | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                        0,
                        scratchBuffer,
                        pStagingAllocation
                );

                // Fill info
                accelerationStructureBuildGeometryInfo
                        .scratchData(getDeviceAddress(stack, scratchBuffer.get()))
                        .dstAccelerationStructure(BLAS.handle.get(0));

                VkCommandBuffer cmdBuffer = beginImmediateCmd();
//            VkCommandBuffer cmdBuffer = createCommandBuffer(getCommandPool(), VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                vkCmdPipelineBarrier(
                        cmdBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_ACCELERATION_STRUCTURE_BUILD_BIT_KHR,
                        0,
                        VkMemoryBarrier
                                .calloc(1, stack)
                                .sType$Default()
                                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                                .dstAccessMask(
                                        VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR |
                                                VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR |
                                                VK_ACCESS_SHADER_READ_BIT),
                        null, null
                );

                vkCmdBuildAccelerationStructuresKHR(
                        cmdBuffer,
                        accelerationStructureBuildGeometryInfo,
                        pointersOfElements(
                                stack,
                                VkAccelerationStructureBuildRangeInfoKHR
                                        .calloc(1, stack)
                                        .primitiveCount(numVertices)
                        )
                );

                endImmediateCmd();
                scratchBuffer.clear();
                rtGeometry.free();

                return;
            }
        } else {

        }
    }

    public static int getVertexFormat(MeshData.DrawState drawState) {
        int vertexFormat = -1;

        VertexFormat drawStateFormat = drawState.format();
        for (int i = 0; i < drawStateFormat.getElements().size(); i++) {
            VertexFormatElement formatElement = drawStateFormat.getElements().get(i);
            VertexFormatElement.Usage usage = formatElement.usage();
            VertexFormatElement.Type type = formatElement.type();

            if (usage == VertexFormatElement.Usage.POSITION) {
                switch (type) {
                    case FLOAT -> {
                        vertexFormat = VK_FORMAT_R32G32B32_SFLOAT;
                    }
                    case SHORT -> {
                        vertexFormat = VK_FORMAT_R16G16B16A16_SINT;
                    }
                    case BYTE -> {
                        vertexFormat = VK_FORMAT_R8G8B8A8_SINT;
                    }
                    default -> {
                        vertexFormat = -1;
                    }
                }
            }
        }

        return vertexFormat;
    }

}

class AccelerationStructure {
    public LongBuffer handle;
    public long deviceAddress;
    public Buffer buffer;

    AccelerationStructure() {
        this.handle = null;
        this.deviceAddress = 0;
        this.buffer = new IndexBuffer(1, MemoryTypes.GPU_MEM, true);
    }
}

class RTGeometry {
    public VertexBuffer vertexBuffer = null;
    public IndexBuffer indexBuffer = null;
    public int numPrimitive = 0;

    RTGeometry(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int numPrimitive) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.numPrimitive = numPrimitive;
    }

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
