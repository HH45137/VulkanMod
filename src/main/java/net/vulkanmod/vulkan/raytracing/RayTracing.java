package net.vulkanmod.vulkan.raytracing;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;

import static org.lwjgl.vulkan.VK12.*;


public class RayTracing {

    public static boolean isEnabledVulkanRayTracing() {
        return true;
    }

    public static ArrayList<String> getVulkanRayTracingRequiredExtensions() {
        ArrayList<String> extensions = new ArrayList<>();

        extensions.add(KHRAccelerationStructure.VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME);
        extensions.add(KHRRayTracingPipeline.VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME);
        extensions.add(KHRRayQuery.VK_KHR_RAY_QUERY_EXTENSION_NAME);
        extensions.add(KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME);
        extensions.add(KHRBufferDeviceAddress.VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME);
        extensions.add(EXTDescriptorIndexing.VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME);

        return extensions;
    }

    public static boolean isPhyDeviceSupportedVulkanRayTracingFeatures(VkPhysicalDevice phyDevice) {
        boolean isSupported = true;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceRayTracingPipelineFeaturesKHR rtPipelineFeatures = VkPhysicalDeviceRayTracingPipelineFeaturesKHR.calloc(stack)
                    .sType(KHRRayTracingPipeline.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_FEATURES_KHR);
            VkPhysicalDeviceAccelerationStructureFeaturesKHR asFeatures = VkPhysicalDeviceAccelerationStructureFeaturesKHR.calloc(stack)
                    .sType(KHRAccelerationStructure.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR)
                    .pNext(rtPipelineFeatures.address());

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(asFeatures.address());

            vkGetPhysicalDeviceFeatures2(phyDevice, features2);

            if (!asFeatures.accelerationStructure() || !rtPipelineFeatures.rayTracingPipeline()) {
                isSupported = false;
            }
        }

        return isSupported;
    }
}
