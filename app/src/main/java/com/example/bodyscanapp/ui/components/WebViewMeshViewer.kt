package com.example.bodyscanapp.ui.components

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

/**
 * WebView-based 3D mesh viewer using Google's ModelViewer
 * This is the simplest and most reliable solution - works everywhere
 * Reference: https://modelviewer.dev/
 */
@Composable
fun WebViewMeshViewer(
    glbBytes: ByteArray,
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(glbBytes) {
        if (glbBytes.isEmpty()) {
            errorMessage = "GLB mesh data is empty"
            onError?.invoke(errorMessage!!)
            return@LaunchedEffect
        }
        
        // Validate GLB header
        if (glbBytes.size < 12) {
            errorMessage = "Invalid GLB file: too small"
            onError?.invoke(errorMessage!!)
            return@LaunchedEffect
        }
        
        val magic = ByteArray(4)
        System.arraycopy(glbBytes, 0, magic, 0, 4)
        val magicInt = (magic[0].toInt() and 0xFF) or 
                      ((magic[1].toInt() and 0xFF) shl 8) or
                      ((magic[2].toInt() and 0xFF) shl 16) or
                      ((magic[3].toInt() and 0xFF) shl 24)
        
        if (magicInt != 0x46546C67) {
            errorMessage = "Invalid GLB file: incorrect magic number"
            onError?.invoke(errorMessage!!)
            return@LaunchedEffect
        }
    }
    
    val tempFile = remember { mutableStateOf<File?>(null) }
    
    // Clean up temp file when composable is disposed
    DisposableEffect(glbBytes) {
        onDispose {
            tempFile.value?.delete()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                webViewClient = WebViewClient()
                
                if (glbBytes.isNotEmpty() && errorMessage == null) {
                    loadGlbModel(ctx, glbBytes, tempFile)
                }
            }
        },
        modifier = modifier,
        update = { view ->
            if (view is WebView && glbBytes.isNotEmpty() && errorMessage == null) {
                view.loadGlbModel(context, glbBytes, tempFile)
            }
        }
    )
    
    if (errorMessage != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = errorMessage ?: "Unknown error",
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

private fun WebView.loadGlbModel(context: Context, glbBytes: ByteArray, tempFile: androidx.compose.runtime.MutableState<File?>) {
    try {
        // Convert GLB to base64 for embedding
        val base64 = Base64.encodeToString(glbBytes, Base64.NO_WRAP)
        
        Log.d("WebViewMeshViewer", "Preparing GLB model: ${glbBytes.size} bytes, base64 length: ${base64.length}")
        
        // Create HTML with ModelViewer - use blob URL approach via JavaScript
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script type="module" src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.4.0/model-viewer.min.js"></script>
                <script>
                    // Fallback: Check if model-viewer loaded
                    window.addEventListener('load', () => {
                        setTimeout(() => {
                            if (!customElements.get('model-viewer')) {
                                console.error('model-viewer not loaded after 2 seconds');
                                const statusDiv = document.getElementById('status');
                                if (statusDiv) {
                                    statusDiv.textContent = 'Error: model-viewer script failed to load';
                                    statusDiv.style.background = 'rgba(255,0,0,0.5)';
                                }
                            } else {
                                console.log('model-viewer custom element registered');
                                window.modelViewerReady = true;
                                if (window.onModelViewerReady) {
                                    window.onModelViewerReady();
                                }
                            }
                        }, 2000);
                    });
                </script>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        background: #1E1E1E;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    model-viewer {
                        width: 100%;
                        height: 100vh;
                        background: #1E1E1E;
                    }
                    #status {
                        position: absolute;
                        top: 10px;
                        left: 10px;
                        color: white;
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        z-index: 1000;
                        background: rgba(0,0,0,0.5);
                        padding: 5px 10px;
                        border-radius: 5px;
                    }
                </style>
            </head>
            <body>
                <div id="status">Loading model...</div>
                <model-viewer 
                    id="viewer"
                    alt="3D Body Scan"
                    auto-rotate
                    auto-rotate-delay="1000"
                    camera-controls
                    camera-orbit="0deg 75deg 3m"
                    field-of-view="45deg"
                    min-camera-orbit="auto auto 0.5m"
                    max-camera-orbit="auto auto 10m"
                    camera-target="0m 0.675m 0m"
                    exposure="1.0"
                    shadow-intensity="1"
                    environment-image="neutral"
                    touch-action="none"
                    interaction-policy="allow-when-focused"
                    style="width: 100%; height: 100%; background: #1E1E1E;">
                </model-viewer>
                <script>
                    const statusDiv = document.getElementById('status');
                    
                    function updateStatus(msg) {
                        console.log('Status:', msg);
                        if (statusDiv) statusDiv.textContent = msg;
                    }
                    
                    // Wait for model-viewer to be defined
                    function waitForModelViewer(callback) {
                        if (customElements.get('model-viewer')) {
                            callback();
                        } else if (window.modelViewerReady) {
                            // Script loaded but custom element not registered yet
                            setTimeout(() => waitForModelViewer(callback), 100);
                        } else {
                            updateStatus('Waiting for model-viewer script...');
                            // Set up callback for when script loads
                            window.onModelViewerReady = () => {
                                setTimeout(() => waitForModelViewer(callback), 100);
                            };
                            // Timeout after 5 seconds
                            setTimeout(() => {
                                if (!customElements.get('model-viewer')) {
                                    updateStatus('Error: model-viewer failed to load');
                                    console.error('model-viewer custom element not available after timeout');
                                }
                            }, 5000);
                        }
                    }
                    
                    waitForModelViewer(() => {
                        updateStatus('Model-viewer ready, processing GLB...');
                        
                        try {
                            // Convert base64 to blob and create blob URL
                            const base64 = '$base64';
                            console.log('Base64 length:', base64.length);
                            
                            const binaryString = atob(base64);
                            const bytes = new Uint8Array(binaryString.length);
                            for (let i = 0; i < binaryString.length; i++) {
                                bytes[i] = binaryString.charCodeAt(i);
                            }
                            
                            // Verify GLB magic number
                            if (bytes.length >= 4) {
                                const magic = String.fromCharCode(bytes[0], bytes[1], bytes[2], bytes[3]);
                                console.log('GLB magic:', magic, 'Expected: glTF');
                                if (magic !== 'glTF') {
                                    console.error('Invalid GLB magic number:', magic);
                                    updateStatus('Error: Invalid GLB format');
                                    return;
                                }
                            }
                            
                            // Verify GLB structure - check for JSON chunk
                            if (bytes.length >= 20) {
                                const jsonChunkLength = bytes[12] | (bytes[13] << 8) | (bytes[14] << 16) | (bytes[15] << 24);
                                const jsonChunkType = String.fromCharCode(bytes[16], bytes[17], bytes[18], bytes[19]);
                                console.log('JSON chunk length:', jsonChunkLength, 'type:', jsonChunkType);
                                
                                if (jsonChunkType !== 'JSON') {
                                    console.error('Invalid JSON chunk type:', jsonChunkType);
                                    updateStatus('Error: Invalid GLB structure');
                                    return;
                                }
                            }
                            
                            const blob = new Blob([bytes], { type: 'model/gltf-binary' });
                            const blobUrl = URL.createObjectURL(blob);
                            console.log('Created blob URL:', blobUrl);
                            
                            // Load the model
                            const viewer = document.getElementById('viewer');
                            if (!viewer) {
                                console.error('Viewer element not found!');
                                updateStatus('Error: Viewer element not found');
                                return;
                            }
                            
                            updateStatus('Loading model...');
                            
                            // Set up error handler first
                            viewer.addEventListener('error', (event) => {
                                console.error('Model loading error:', event.detail);
                                console.error('Blob URL:', blobUrl);
                                console.error('Blob size:', blob.size, 'bytes');
                                updateStatus('Error loading model: ' + (event.detail?.message || 'Unknown error'));
                            });
                            
                            // Set up load handler
                            viewer.addEventListener('load', () => {
                                console.log('Model loaded successfully');
                                updateStatus('Model loaded - adjusting camera...');
                                
                                // Force camera to fit model and ensure it's visible
                                setTimeout(() => {
                                    try {
                                        // Get model dimensions first
                                        let dims = null;
                                        if (viewer.getDimensions) {
                                            dims = viewer.getDimensions();
                                            console.log('Model dimensions:', dims);
                                        }
                                        
                                        // Calculate appropriate camera distance based on model size
                                        let cameraDistance = '3m'; // Default
                                        let cameraTarget = '0m 0.675m 0m'; // Default to center at half height
                                        
                                        if (dims) {
                                            const maxDim = Math.max(
                                                Math.abs(dims.x || 0), 
                                                Math.abs(dims.y || 0), 
                                                Math.abs(dims.z || 0)
                                            );
                                            if (maxDim > 0) {
                                                // Set camera distance to 2.5x the model size
                                                cameraDistance = (maxDim * 2.5).toFixed(2) + 'm';
                                                // Center target at model center (half of Y dimension)
                                                const centerY = (dims.y || 1.35) / 2;
                                                cameraTarget = '0m ' + centerY.toFixed(2) + 'm 0m';
                                                console.log('Calculated camera distance:', cameraDistance, 'target:', cameraTarget, 'from maxDim:', maxDim);
                                            }
                                        }
                                        
                                        // Set camera target first
                                        if (viewer.cameraTarget !== undefined) {
                                            viewer.cameraTarget = cameraTarget;
                                            console.log('Set camera target to:', cameraTarget);
                                        }
                                        
                                        // Set camera orbit with calculated distance
                                        viewer.cameraOrbit = `0deg 75deg ${cameraDistance}`;
                                        console.log('Set camera orbit to:', viewer.cameraOrbit);
                                        
                                        // Force update
                                        if (viewer.updateFraming) {
                                            viewer.updateFraming();
                                        }
                                        
                                        console.log('Camera framing updated');
                                        
                                        if (dims) {
                                            updateStatus('Model ready - ' + JSON.stringify(dims));
                                        } else {
                                            updateStatus('Model ready');
                                        }
                                    } catch (e) {
                                        console.log('Error updating camera:', e);
                                        updateStatus('Model loaded but camera error: ' + e.message);
                                    }
                                }, 200);
                                
                                URL.revokeObjectURL(blobUrl); // Clean up
                            });
                            
                            // Listen for model-loaded event (fired after model is fully parsed)
                            viewer.addEventListener('model-loaded', () => {
                                console.log('Model fully loaded and parsed');
                                updateStatus('Model parsed - finalizing...');
                                
                                // Ensure model is visible
                                setTimeout(() => {
                                    try {
                                        viewer.cameraOrbit = '0deg 75deg auto';
                                        if (viewer.updateFraming) {
                                            viewer.updateFraming();
                                        }
                                        console.log('Final camera adjustment complete');
                                        
                                        // Hide status after a delay
                                        setTimeout(() => {
                                            if (statusDiv) statusDiv.style.display = 'none';
                                        }, 2000);
                                    } catch (e) {
                                        console.error('Error in final camera adjustment:', e);
                                    }
                                }, 300);
                            });
                            
                            // Additional debugging
                            viewer.addEventListener('progress', (event) => {
                                const progress = event.detail?.totalProgress || 0;
                                console.log('Model loading progress:', progress);
                                if (progress > 0 && progress < 1) {
                                    updateStatus('Loading... ' + Math.round(progress * 100) + '%');
                                }
                            });
                            
                            // Set the source to trigger loading
                            viewer.src = blobUrl;
                            console.log('Set viewer.src to:', blobUrl);
                            
                            // Timeout to detect if loading is stuck
                            setTimeout(() => {
                                if (statusDiv && statusDiv.textContent.includes('Loading')) {
                                    console.warn('Model loading timeout - checking status');
                                    updateStatus('Loading timeout - check console for errors');
                                }
                            }, 10000);
                            
                        } catch (e) {
                            console.error('Error creating blob:', e);
                            console.error('Stack:', e.stack);
                            updateStatus('Error: ' + e.message);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        
        // Load HTML directly
        loadDataWithBaseURL("https://modelviewer.dev", html, "text/html", "UTF-8", null)
        
        Log.d("WebViewMeshViewer", "Loaded GLB model via blob URL: ${glbBytes.size} bytes")
    } catch (e: Exception) {
        Log.e("WebViewMeshViewer", "Error loading GLB in WebView", e)
        tempFile.value?.delete()
    }
}

