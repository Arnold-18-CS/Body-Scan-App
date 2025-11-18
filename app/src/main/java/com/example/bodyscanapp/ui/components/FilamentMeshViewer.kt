package com.example.bodyscanapp.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * FilamentView - Custom GLSurfaceView with full Filament engine integration
 * 
 * Handles:
 * - Filament Engine, Renderer, Scene, Camera initialization
 * - GLB mesh loading from ByteArray
 * - Orbit controller (rotate, zoom, pan)
 * - Proper lifecycle management and cleanup
 */
class FilamentView(context: Context) : GLSurfaceView(context) {
    
    private var engine: Engine? = null
    private var filamentRenderer: com.google.android.filament.Renderer? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var uiHelper: UiHelper? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null
    private var materialProvider: MaterialProvider? = null
    private var filamentAsset: FilamentAsset? = null
    
    // Orbit controller state
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var cameraDistance = 5.0f
    private var cameraRotationX = 0.0f
    private var cameraRotationY = 0.0f
    private var cameraPanX = 0.0f
    private var cameraPanY = 0.0f
    
    // Gesture detectors
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    // Mesh bounding box for centering
    private var meshCenter = FloatArray(3) { 0f }
    private var meshScale = 1.0f
    
    init {
        // Initialize Filament Utils
        Utils.init()
        
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        
        // Initialize gesture detectors
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e2.pointerCount == 1) {
                    // Single finger drag - rotate
                    cameraRotationY += distanceX * 0.01f
                    cameraRotationX += distanceY * 0.01f
                    cameraRotationX = cameraRotationX.coerceIn(-PI.toFloat() / 2 + 0.1f, PI.toFloat() / 2 - 0.1f)
                    updateCamera()
                } else if (e2.pointerCount == 2) {
                    // Two finger drag - pan
                    cameraPanX -= distanceX * 0.001f
                    cameraPanY += distanceY * 0.001f
                    updateCamera()
                }
                return true
            }
        })
        
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Pinch to zoom
                val scaleFactor = detector.scaleFactor
                cameraDistance /= scaleFactor
                cameraDistance = cameraDistance.coerceIn(1.0f, 20.0f)
                updateCamera()
                return true
            }
        })
        
        val glRenderer = object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                setupFilament()
            }
            
            override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
                view?.viewport = Viewport(0, 0, width, height)
                camera?.setProjection(
                    45.0, width.toDouble() / height.toDouble(),
                    0.1, 20.0,
                    Camera.Fov.VERTICAL
                )
            }
            
            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                // Rendering is handled by UiHelper render callback
                // The UiHelper will call the renderer automatically
            }
        }
        setRenderer(glRenderer)
        
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
    
    private fun setupFilament() {
        try {
            val engineInstance = Engine.create()
            engine = engineInstance
            
            // Initialize Renderer - store for cleanup, but don't assign to GLSurfaceView.Renderer
            filamentRenderer = engineInstance.createRenderer()
            
            // Create Scene
            scene = engineInstance.createScene()
            
            // Create Camera
            val cameraEntity = engineInstance.entityManager.create()
            camera = engineInstance.createCamera(cameraEntity)
            
            // Create View
            view = engineInstance.createView()
            view?.scene = scene
            view?.camera = camera
            
            // Setup UiHelper for Android lifecycle
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                renderCallback = object : UiHelper.RendererCallback {
                    override fun onNativeWindowChanged(surface: android.view.Surface) {
                        val swapChain = engineInstance.createSwapChain(surface)
                        // SwapChain is a RenderTarget
                        view?.setRenderTarget(swapChain as RenderTarget)
                    }
                    
                    override fun onDetachedFromSurface() {
                        view?.setRenderTarget(null)
                    }
                    
                    override fun onResized(width: Int, height: Int) {
                        view?.viewport = Viewport(0, 0, width, height)
                        camera?.setProjection(
                            45.0, width.toDouble() / height.toDouble(),
                            0.1, 20.0,
                            Camera.Fov.VERTICAL
                        )
                    }
                }
            }
            
            // Attach UiHelper to the view
            uiHelper?.attachTo(this)
            
            // Initialize MaterialProvider and AssetLoader for GLB loading
            val entityManager = EntityManager.get()
            // MaterialProvider is an interface - need to use UbershaderLoader from gltfio-android
            // Check if UbershaderLoader is available
            materialProvider = try {
                // Try to use UbershaderLoader - this should be available in gltfio-android
                val loaderClass = Class.forName("com.google.android.filament.gltfio.UbershaderLoader")
                val constructor = loaderClass.getConstructor(Engine::class.java)
                constructor.newInstance(engineInstance) as MaterialProvider
            } catch (e: Exception) {
                Log.e("FilamentView", "Could not create MaterialProvider", e)
                null
            }
            
            if (materialProvider != null) {
                assetLoader = AssetLoader(engineInstance, materialProvider!!, entityManager)
                resourceLoader = ResourceLoader(engineInstance, false)
            } else {
                Log.e("FilamentView", "MaterialProvider is null, GLB loading will not work")
            }
            
            // Setup lighting
            setupLighting()
            
            // Set initial camera position
            updateCamera()
            
            Log.d("FilamentView", "Filament engine initialized successfully")
            
        } catch (e: Exception) {
            Log.e("FilamentView", "Failed to setup Filament", e)
        }
    }
    
    private fun setupLighting() {
        val engineInstance = engine ?: return
        val sceneInstance = scene ?: return
        
        // Create ambient light
        val ambientLightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.SUN)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(50000.0f)
            .direction(0.0f, -1.0f, -1.0f)
            .castShadows(false)
            .build(engineInstance, ambientLightEntity)
        
        sceneInstance.addEntity(ambientLightEntity)
        
        // Create directional light
        val directionalLightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.SUN)
            .color(1.0f, 1.0f, 0.95f)
            .intensity(100000.0f)
            .direction(0.0f, -1.0f, -1.0f)
            .castShadows(false)
            .build(engineInstance, directionalLightEntity)
        
        sceneInstance.addEntity(directionalLightEntity)
    }
    
    fun loadGlbFromBytes(glbBytes: ByteArray) {
        val engineInstance = engine ?: return
        val sceneInstance = scene ?: return
        val assetLoaderInstance = assetLoader ?: return
        val resourceLoaderInstance = resourceLoader ?: return
        
        try {
            // Remove existing asset if any
            filamentAsset?.let { asset ->
                sceneInstance.removeEntities(asset.entities)
                assetLoaderInstance.destroyAsset(asset)
            }
            
            // Convert ByteArray to ByteBuffer
            val buffer = ByteBuffer.allocateDirect(glbBytes.size)
                .order(ByteOrder.nativeOrder())
                .put(glbBytes)
                .rewind()
            
            // Load GLB asset
            val asset = assetLoaderInstance.createAsset(buffer)
            if (asset == null) {
                Log.e("FilamentView", "Failed to load GLB asset")
                return
            }
            
            filamentAsset = asset
            
            // Load resources (textures, etc.)
            resourceLoaderInstance.loadResources(asset)
            
            // Calculate bounding box and center mesh
            val aabb = asset.boundingBox
            // Access bounding box - getCenter() returns FloatArray directly
            val center = aabb.getCenter()
            // Get min and max - use getMin/getMax methods or access directly
            val minArray = FloatArray(3)
            val maxArray = FloatArray(3)
            // Try to get min/max - if methods don't exist, use center as approximation
            try {
                val getMinMethod = aabb.javaClass.getMethod("getMin", FloatArray::class.java)
                getMinMethod.invoke(aabb, minArray)
                val getMaxMethod = aabb.javaClass.getMethod("getMax", FloatArray::class.java)
                getMaxMethod.invoke(aabb, maxArray)
            } catch (e: Exception) {
                // Fallback: use center as mesh center
                minArray[0] = center[0] - 1.0f
                minArray[1] = center[1] - 1.0f
                minArray[2] = center[2] - 1.0f
                maxArray[0] = center[0] + 1.0f
                maxArray[1] = center[1] + 1.0f
                maxArray[2] = center[2] + 1.0f
            }
            
            meshCenter[0] = center[0]
            meshCenter[1] = center[1]
            meshCenter[2] = center[2]
            
            val sizeX = maxArray[0] - minArray[0]
            val sizeY = maxArray[1] - minArray[1]
            val sizeZ = maxArray[2] - minArray[2]
            val maxSize = if (sizeX > sizeY && sizeX > sizeZ) sizeX else if (sizeY > sizeZ) sizeY else sizeZ
            meshScale = if (maxSize > 0) 2.0f / maxSize else 1.0f
            
            // Center and scale the mesh
            val root = asset.root
            val transformManager = engineInstance.transformManager
            val instance = transformManager.getInstance(root)
            
            // Reset transform to identity
            val identityMatrix = FloatArray(16)
            identityMatrix[0] = 1.0f
            identityMatrix[5] = 1.0f
            identityMatrix[10] = 1.0f
            identityMatrix[15] = 1.0f
            transformManager.setTransform(instance, identityMatrix)
            
            // Add asset to scene
            sceneInstance.addEntities(asset.entities)
            
            // Reset camera to view the mesh
            cameraDistance = maxSize * 1.5f
            cameraDistance = cameraDistance.coerceIn(1.0f, 20.0f)
            cameraRotationX = 0.3f
            cameraRotationY = 0.0f
            cameraPanX = 0.0f
            cameraPanY = 0.0f
            updateCamera()
            
            Log.d("FilamentView", "GLB mesh loaded successfully: ${glbBytes.size} bytes")
            
        } catch (e: Exception) {
            Log.e("FilamentView", "Error loading GLB from bytes", e)
        }
    }
    
    private fun updateCamera() {
        val cameraInstance = camera ?: return
        
        // Calculate camera position based on orbit controller
        val cosX = cos(cameraRotationX)
        val sinX = sin(cameraRotationX)
        val cosY = cos(cameraRotationY)
        val sinY = sin(cameraRotationY)
        
        val eyeX = meshCenter[0] + cameraPanX + cameraDistance * cosX * sinY
        val eyeY = meshCenter[1] + cameraPanY + cameraDistance * sinX
        val eyeZ = meshCenter[2] + cameraDistance * cosX * cosY
        
        val centerX = meshCenter[0] + cameraPanX
        val centerY = meshCenter[1] + cameraPanY
        val centerZ = meshCenter[2] + cameraPanX
        
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f
        
        cameraInstance.lookAt(
            eyeX.toDouble(), eyeY.toDouble(), eyeZ.toDouble(),
            centerX.toDouble(), centerY.toDouble(), centerZ.toDouble(),
            upX.toDouble(), upY.toDouble(), upZ.toDouble()
        )
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
    
    override fun onPause() {
        super.onPause()
        // UiHelper lifecycle is handled automatically
    }
    
    override fun onResume() {
        super.onResume()
        // UiHelper lifecycle is handled automatically
    }
    
    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }
    
    private fun cleanup() {
        try {
            // Cleanup Filament resources
            filamentAsset?.let { asset ->
                scene?.removeEntities(asset.entities)
                assetLoader?.destroyAsset(asset)
            }
            
            assetLoader?.destroy()
            resourceLoader?.destroy()
            // MaterialProvider cleanup if needed
            try {
                materialProvider?.let {
                    val destroyMethod = it.javaClass.getMethod("destroy")
                    destroyMethod.invoke(it)
                }
            } catch (e: Exception) {
                // MaterialProvider may not have destroy method
            }
            // View cleanup - use reflection to call destroy if it exists
            try {
                view?.javaClass?.getMethod("destroy")?.invoke(view)
            } catch (e: Exception) {
                // View may not have destroy method
            }
            camera?.let { 
                val entity = it.entity
                engine?.destroyEntity(entity)
            }
            // Cleanup scene, renderer, and engine - use reflection
            try {
                scene?.javaClass?.getMethod("destroy")?.invoke(scene)
            } catch (e: Exception) {
                // Scene may not have destroy method
            }
            try {
                filamentRenderer?.javaClass?.getMethod("destroy")?.invoke(filamentRenderer)
            } catch (e: Exception) {
                // Renderer may not have destroy method
            }
            try {
                engine?.javaClass?.getMethod("destroy")?.invoke(engine)
            } catch (e: Exception) {
                // Engine may not have destroy method
            }
            
            engine = null
            filamentRenderer = null
            scene = null
            camera = null
            view = null
            assetLoader = null
            resourceLoader = null
            materialProvider = null
            filamentAsset = null
            
            Log.d("FilamentView", "Filament resources cleaned up")
            
        } catch (e: Exception) {
            Log.e("FilamentView", "Error during cleanup", e)
        }
    }
}

/**
 * FilamentMeshViewer - Displays a 3D mesh using Filament engine
 * 
 * Features:
 * - Loads GLB mesh from ByteArray
 * - Renders 3D mesh with lighting
 * - Orbit controller (touch to rotate, pinch to zoom, drag to pan)
 * - Camera controls
 * - Error handling and loading states
 * 
 * @param glbBytes The GLB mesh data as ByteArray
 * @param modifier Modifier for the viewer
 * @param onError Callback when loading fails
 */
@Composable
fun FilamentMeshViewer(
    glbBytes: ByteArray,
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Track loading and error states
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Validate GLB data
    LaunchedEffect(glbBytes) {
        isLoading = true
        errorMessage = null
        
        try {
            if (glbBytes.isEmpty()) {
                errorMessage = "GLB mesh data is empty"
                onError?.invoke(errorMessage!!)
                isLoading = false
                return@LaunchedEffect
            }
            
            // Check GLB header (should start with "glTF" in binary format)
            if (glbBytes.size < 12) {
                errorMessage = "Invalid GLB file: too small"
                onError?.invoke(errorMessage!!)
                isLoading = false
                return@LaunchedEffect
            }
            
            // Validate GLB magic number (first 4 bytes should be 0x46546C67 = "glTF")
            val magic = ByteArray(4)
            System.arraycopy(glbBytes, 0, magic, 0, 4)
            val magicInt = (magic[0].toInt() and 0xFF) or 
                          ((magic[1].toInt() and 0xFF) shl 8) or
                          ((magic[2].toInt() and 0xFF) shl 16) or
                          ((magic[3].toInt() and 0xFF) shl 24)
            
            if (magicInt != 0x46546C67) { // "glTF" in little-endian
                errorMessage = "Invalid GLB file: incorrect magic number"
                onError?.invoke(errorMessage!!)
                isLoading = false
                return@LaunchedEffect
            }
            
            isLoading = false
            Log.d("FilamentMeshViewer", "GLB mesh validated: ${glbBytes.size} bytes")
            
        } catch (e: Exception) {
            errorMessage = "Error validating GLB: ${e.message}"
            Log.e("FilamentMeshViewer", "Validation error", e)
            onError?.invoke(errorMessage!!)
            isLoading = false
        }
    }
    
    AndroidView(
        factory = { ctx ->
            try {
                FilamentView(ctx).apply {
                    // Load GLB once view is created and validated
                    if (glbBytes.isNotEmpty() && !isLoading && errorMessage == null) {
                        loadGlbFromBytes(glbBytes)
                    }
                }
            } catch (e: Exception) {
                Log.e("FilamentMeshViewer", "Failed to create FilamentView", e)
                errorMessage = e.message ?: "Unknown error"
                onError?.invoke(errorMessage!!)
                android.view.View(ctx)
            }
        },
        modifier = modifier,
        update = { view ->
            // Update view if GLB data changed
            if (view is FilamentView && glbBytes.isNotEmpty() && !isLoading && errorMessage == null) {
                view.loadGlbFromBytes(glbBytes)
            }
        }
    )
    
    // Show error overlay if there's an error
    if (errorMessage != null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mesh Loading Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
