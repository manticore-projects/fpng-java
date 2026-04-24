.. raw:: html

    <div id="floating-toc">
        <div class="search-container">
            <input type="button" id="toc-hide-show-btn"></input>
            <input type="text" id="toc-search" placeholder="Search" />
        </div>
        <ul id="toc-list"></ul>
    </div>



#######################################################################
API 0.10.0-SNAPSHOT
#######################################################################

Base Package: com.manticore.tools


..  _com.manticore.tools:
***********************************************************************
Base
***********************************************************************

..  _com.manticore.tools.FPNGE.FPNGECicpColorspace

=======================================================================
FPNGE.FPNGECicpColorspace
=======================================================================

[FPNGE_CICP_NONE, FPNGE_CICP_PQ]


..  _com.manticore.tools.FPNGE.FPNGEOptionsPredictor

=======================================================================
FPNGE.FPNGEOptionsPredictor
=======================================================================

[FPNGE_PREDICTOR_FIXED_NOOP, FPNGE_PREDICTOR_FIXED_SUB, FPNGE_PREDICTOR_FIXED_TOP, FPNGE_PREDICTOR_FIXED_AVG, FPNGE_PREDICTOR_FIXED_PAETH, FPNGE_PREDICTOR_APPROX, FPNGE_PREDICTOR_BEST]


..  _com.manticore.tools.FPNGE.CharArray:

=======================================================================
FPNGE.CharArray
=======================================================================

*extends:* Structure 

| **CharArray** ()



                |          returns :ref:`List<java.util.List>`


                
            
..  _com.manticore.tools.FPNGE.FPNGEOptions:

=======================================================================
FPNGE.FPNGEOptions
=======================================================================

*extends:* Structure 

| **FPNGEOptions** ()



                |          returns :ref:`List<java.util.List>`


                
            
..  _com.manticore.tools.FPNGE:
=======================================================================
FPNGE
=======================================================================

*provides:*  

| **fpng_init** ()


| **FPNGEEncode1** (bytes_per_channel, num_channels, pImage, width, height) → :ref:`CharArray<com.manticore.tools.FPNGE.CharArray>`
|          NativeLong bytes_per_channel
|          NativeLong num_channels
|          byte pImage
|          NativeLong width
|          NativeLong height
|          returns :ref:`CharArray<com.manticore.tools.FPNGE.CharArray>`



| **encode** (image, numberOfChannels, flags) → byte
|          :ref:`BufferedImage<java.awt.image.BufferedImage>` image
|          int numberOfChannels
|          int flags
|          returns byte



