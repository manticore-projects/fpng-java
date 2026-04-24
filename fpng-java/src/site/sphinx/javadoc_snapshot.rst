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

..  _com.manticore.tools.FPNGEncoder:
=======================================================================
FPNGEncoder
=======================================================================

*provides:*  

| **fpng_init** ()


| **fpng_encode_image_to_memory** (pImage, w, h, num_chans, flags) → ByteArray
|          byte pImage
|          int w
|          int h
|          int num_chans
|          int flags
|          returns ByteArray



| **encode** (image, numberOfChannels, flags) → byte
|          :ref:`BufferedImage<java.awt.image.BufferedImage>` image
|          int numberOfChannels
|          int flags
|          returns byte



