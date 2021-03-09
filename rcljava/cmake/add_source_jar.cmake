#
# Creates a sources jar from list of source files
#
# :param VERSION: target Java version, default: "${CMAKE_JAVA_TARGET_VERSION}"
# :type VERSION: string
# :param OUTPUT_DIR: Output directory
# :type OUTPUT_DIR: string
# :param OUTPUT_NAME: Name of Jar
# :type OUTPUT_NAME: string
# :param MANIFEST: Add Manifest?
# :type MANIFEST: boolean
# :param SOURCES: Sources files
# :type SOURCES: String list with file names
#
# @public
#
function(add_source_jar _TARGET_NAME)

  cmake_parse_arguments(_add_source_jar
    ""
    "VERSION;OUTPUT_DIR;OUTPUT_NAME;MANIFEST"
    "SOURCES"
    ${ARGN}
  )

    # In CMake < 2.8.12, add_jar used variables which were set prior to calling
    # add_jar for customizing the behavior of add_jar. In order to be backwards
    # compatible, check if any of those variables are set, and use them to
    # initialize values of the named arguments. (Giving the corresponding named
    # argument will override the value set here.)
    #
    # New features should use named arguments only.
    if(NOT DEFINED _add_source_jar_VERSION AND DEFINED CMAKE_JAVA_TARGET_VERSION)
        set(_add_source_jar_VERSION "${CMAKE_JAVA_TARGET_VERSION}")
    endif()
    if(NOT DEFINED _add_source_jar_OUTPUT_DIR AND DEFINED CMAKE_JAVA_TARGET_OUTPUT_DIR)
        set(_add_source_jar_OUTPUT_DIR "${CMAKE_JAVA_TARGET_OUTPUT_DIR}")
    endif()
    if(NOT DEFINED _add_source_jar_OUTPUT_NAME AND DEFINED CMAKE_JAVA_TARGET_OUTPUT_NAME)
        set(_add_source_jar_OUTPUT_NAME "${CMAKE_JAVA_TARGET_OUTPUT_NAME}")
        # reset
        set(CMAKE_JAVA_TARGET_OUTPUT_NAME)
    endif()

    set(_JAVA_SOURCE_FILES ${_add_source_jar_SOURCES} ${_add_source_jar_UNPARSED_ARGUMENTS})

    if(NOT DEFINED _add_source_jar_OUTPUT_DIR)
        set(_add_source_jar_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR})
    else()
        get_filename_component(_add_source_jar_OUTPUT_DIR ${_add_source_jar_OUTPUT_DIR} ABSOLUTE)
    endif()
    # ensure output directory exists
    file (MAKE_DIRECTORY "${_add_source_jar_OUTPUT_DIR}")

    if(_add_source_jar_MANIFEST)
        set(_MANIFEST_OPTION m)
        get_filename_component (_MANIFEST_VALUE "${_add_source_jar_MANIFEST}" ABSOLUTE)
    endif()

    set(CMAKE_JAVA_CLASS_OUTPUT_PATH "${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/${_TARGET_NAME}.dir")

    set(_JAVA_TARGET_OUTPUT_NAME "${_TARGET_NAME}.jar")
    if(_add_source_jar_OUTPUT_NAME AND _add_source_jar_VERSION)
        set(_JAVA_TARGET_OUTPUT_NAME "${_add_source_jar_OUTPUT_NAME}-${_add_source_jar_VERSION}.jar")
        set(_JAVA_TARGET_OUTPUT_LINK "${_add_source_jar_OUTPUT_NAME}.jar")
    elseif(_add_source_jar_VERSION)
        set(_JAVA_TARGET_OUTPUT_NAME "${_TARGET_NAME}-${_add_source_jar_VERSION}.jar")
        set(_JAVA_TARGET_OUTPUT_LINK "${_TARGET_NAME}.jar")
    elseif(_add_source_jar_OUTPUT_NAME)
        set(_JAVA_TARGET_OUTPUT_NAME "${_add_source_jar_OUTPUT_NAME}.jar")
    endif()

    set(_JAVA_COMPILE_FILES)
    set(_JAVA_COMPILE_FILELISTS)
    set(_JAVA_DEPENDS)
    set(_JAVA_COMPILE_DEPENDS)
    set(_JAVA_RESOURCE_FILES)
    set(_JAVA_RESOURCE_FILES_RELATIVE)
    foreach(_JAVA_SOURCE_FILE IN LISTS _JAVA_SOURCE_FILES)
        get_filename_component(_JAVA_EXT ${_JAVA_SOURCE_FILE} EXT)
        get_filename_component(_JAVA_FILE ${_JAVA_SOURCE_FILE} NAME_WE)
        get_filename_component(_JAVA_PATH ${_JAVA_SOURCE_FILE} PATH)
        get_filename_component(_JAVA_FULL ${_JAVA_SOURCE_FILE} ABSOLUTE)

        if(_JAVA_SOURCE_FILE MATCHES "^@(.+)$")
            get_filename_component(_JAVA_FULL ${CMAKE_MATCH_1} ABSOLUTE)
            list(APPEND _JAVA_COMPILE_FILELISTS ${_JAVA_FULL})

        elseif(_JAVA_EXT MATCHES ".java")
            list(APPEND _JAVA_COMPILE_FILES ${_JAVA_SOURCE_FILE})

        elseif(_JAVA_EXT STREQUAL "")
            list(APPEND CMAKE_JAVA_INCLUDE_PATH ${JAVA_JAR_TARGET_${_JAVA_SOURCE_FILE}} ${JAVA_JAR_TARGET_${_JAVA_SOURCE_FILE}_CLASSPATH})
            list(APPEND _JAVA_DEPENDS ${JAVA_JAR_TARGET_${_JAVA_SOURCE_FILE}})

        else ()
            __java_copy_file(${CMAKE_CURRENT_SOURCE_DIR}/${_JAVA_SOURCE_FILE}
                             ${CMAKE_JAVA_CLASS_OUTPUT_PATH}/${_JAVA_SOURCE_FILE}
                             "Copying ${_JAVA_SOURCE_FILE} to the build directory")
            list(APPEND _JAVA_RESOURCE_FILES ${CMAKE_JAVA_CLASS_OUTPUT_PATH}/${_JAVA_SOURCE_FILE})
            list(APPEND _JAVA_RESOURCE_FILES_RELATIVE ${_JAVA_SOURCE_FILE})
        endif()
    endforeach()

    if(_JAVA_COMPILE_FILES OR _JAVA_COMPILE_FILELISTS)

        set (_JAVA_SOURCES_FILELISTS)

        if(_JAVA_COMPILE_FILES)
            # Create the list of files to compile.
            set(_JAVA_SOURCES_FILE ${CMAKE_JAVA_CLASS_OUTPUT_PATH}/java_sources)
            string(REPLACE ";" "\"\n\"" _JAVA_COMPILE_STRING "\"${_JAVA_COMPILE_FILES}\"")
            set(CMAKE_CONFIGURABLE_FILE_CONTENT "${_JAVA_COMPILE_STRING}")
            configure_file("${CMAKE_ROOT}/Modules/CMakeConfigurableFile.in"
              "${_JAVA_SOURCES_FILE}" @ONLY)
            unset(CMAKE_CONFIGURABLE_FILE_CONTENT)
            list (APPEND _JAVA_SOURCES_FILELISTS "@${_JAVA_SOURCES_FILE}")
        endif()
        if(_JAVA_COMPILE_FILELISTS)
            foreach (_JAVA_FILELIST IN LISTS _JAVA_COMPILE_FILELISTS)
                list (APPEND _JAVA_SOURCES_FILELISTS "@${_JAVA_FILELIST}")
            endforeach()
        endif()
    endif()

    # create the jar file
    set(_JAVA_JAR_OUTPUT_PATH
      "${_add_source_jar_OUTPUT_DIR}/${_JAVA_TARGET_OUTPUT_NAME}")

    add_custom_command(
        OUTPUT ${_JAVA_JAR_OUTPUT_PATH}
        COMMAND ${Java_JAR_EXECUTABLE}
            -cf${_ENTRY_POINT_OPTION}${_MANIFEST_OPTION} ${_JAVA_JAR_OUTPUT_PATH} ${_ENTRY_POINT_VALUE} ${_MANIFEST_VALUE}
            ${_JAVA_RESOURCE_FILES_RELATIVE} ${_JAVA_SOURCES_FILELISTS}
        COMMAND ${CMAKE_COMMAND}
            -D_JAVA_TARGET_DIR=${_add_source_jar_OUTPUT_DIR}
            -D_JAVA_TARGET_OUTPUT_NAME=${_JAVA_TARGET_OUTPUT_NAME}
            -D_JAVA_TARGET_OUTPUT_LINK=${_JAVA_TARGET_OUTPUT_LINK}
            -P ${_JAVA_SYMLINK_SCRIPT}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DEPENDS ${_JAVA_RESOURCE_FILES} ${_JAVA_DEPENDS} ${_JAVA_COMPILE_FILES} ${_JAVA_COMPILE_FILELISTS} ${_JAVA_COMPILE_DEPENDS} ${_JAVA_SOURCES_FILE}
        COMMENT "Creating Java archive ${_JAVA_TARGET_OUTPUT_NAME}"
        VERBATIM
    )

    # Add the target and make sure we have the latest resource files.
    add_custom_target(${_TARGET_NAME} ALL DEPENDS ${_JAVA_JAR_OUTPUT_PATH})

    set_property(
        TARGET
            ${_TARGET_NAME}
        PROPERTY
            INSTALL_FILES
                ${_JAVA_JAR_OUTPUT_PATH}
    )

    if(_JAVA_TARGET_OUTPUT_LINK)
        set_property(
            TARGET
                ${_TARGET_NAME}
            PROPERTY
                INSTALL_FILES
                    ${_JAVA_JAR_OUTPUT_PATH}
                    ${_add_source_jar_OUTPUT_DIR}/${_JAVA_TARGET_OUTPUT_LINK}
        )
    endif()

    set_property(
        TARGET
            ${_TARGET_NAME}
        PROPERTY
            JAR_FILE
                ${_JAVA_JAR_OUTPUT_PATH}
    )

    set_property(
        TARGET
            ${_TARGET_NAME}
        PROPERTY
            CLASSDIR
                ${CMAKE_JAVA_CLASS_OUTPUT_PATH}
    )
endfunction()
