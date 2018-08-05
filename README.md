# dummy-files

## About

Allows users to work with a parallel structure of a file tree, without modifying
the original files.

The primary intent of this is to be able to do tests of any functionality that
modifies or manipulates files based on their names and paths, without needing to
consider the files actual content.

This package provides functionality to create a "mirror" of a given subtree.
That means the all the relative paths and filenames will be identical to the
original.

Another way of saying this: if you run `find .` in the original directory, and
then in the newly created directory, the output should be identical.

Obviously, this could simply be achieved with `cp -R`.  But if the files you're
working with are large, and/or on a slow (remote?) file system, that could take
a long time.  It also could take a lot of disk space.  If you're working on
something that is not affected by the actual content of the files, those are
wasted resources.

## Dummies

To create this "mirror" of the given tree, we could just create zero-byte files.

But, we can also do something better.

If the files' content doesn't matter to the program you're going to run over the
files, then we can make the content be anything.  Since the presumed actions of
whatever is operating on these files is to move and rename them in some way,
after those actions are finished, you'll want to be able to analyze what has
changed.

So, it would be very helpful if it were easy to associate the manipulated file
with its original path.  We can make that very easy by simply putting the path
into the content of the file.

For example, if you have a directory with a relative path of
`foo/bar/Some Title.mp3`, the mirror version of that file will be a text file
(which will nonetheless have the `.mp3` file extension), with the contents
`foo/bar/Some Title.mp3`.

Let's say you're running a program which simply eliminates "difficult"
characters like spaces from filenames, in order to make it easier to refer to
them from the command line.  After you've mirrored the directory, and run your
program over the mirror, you can easily find out that the file
`foo/bar/SomeTitle.mp3` was previously located at `foo/bar/Some Title.mp3`, by
looking at its content; e.g., by simply `cat`ting it from the command line.

(This feature also could simply give you assurance that the program is not
modifying the content.)

## Accessing the Functionality

This can be used as a command-line tool, and can create dummy files, restore
them to their original location after they've been manipulated, and produce
reports on how they have changed.

But it's written so that the data to do those things can also be acquired via
public methods.  So if you want to write your own report, you can use this
package as a library and call the method that just returns to you a collection
of the files that exist in the mirror directory, along with their original
location.

## Other Metadata?

The functionality currently deals only with directories and filenames.  It does
not (yet) attempt to preserve file modification/creation/access times, file
access permissions, ownership, or file flags.  It also is not yet aware of
symbolic links.  It isn't aware of named pipes, sockets, device files, or any
other type of "non-file file" that a file system may come up with, and likely
never will be.

## TODO

* replicate file modification and creation time
  * optionally?
* preserve "simple" permissions
  * is the file read-only?
  * is the file executable?
* be aware of symbolic links
  * if there are symoblic links in the source tree, but the destination does not
    suport symbolic links, what do we do?
  * users may want to specify to treat symbolic links as normal files in the
    mirror, even if we are able to create symlinks
* allow filtering of files
  * for example, may want to mirror only files that have one of a limited number
    of file extensions, or perhaps mirror all _except_ certain extensions.
* try to preserve ownership?
  * the program presumably would have to be run as root for this to work
* preserve more complex permissions
  * writable by some, not by others
  * group permissions
  * etc.
* preserve [FreeBSD file flags](https://www.freebsd.org/cgi/man.cgi?query=chflags&sektion=1&manpath=freebsd-release-ports)
* log what was done
  * Logging generally is important, though the trick of putting the original
    location into the file's contents alleviates this, somewhat.  One reason
    logging could help is in order to notice if a file gets deleted.  If the
    report uses the original log file, it can know that not all the files
    originally created are still found.  (If the original source is still
    available, it could also be used for this purpose.)
* size specification (while mirroring)
  * If you want a mirror which preserves sizes, you probably just want to
    `cp -R` the directory.  But maybe not.  Again, the source directory might
    be on a very slow disk.  It could be a lot faster to just fill up a new file
    with zeroes, than to actually obtain the content from the original file.
* use specification to create files
  * Mirroring an existing directory is nice, because it's quick and it allows
    you to work with real-world data, whatever your real-world is.  But it might
    be simpler in some cases to just be able to create an input specification,
    and have the program create the files based on that.  For one instance, if
    you wanted to integrate this with jUnit testing, you'd probably want an
    input specification, not a mirroring.
* size specification (create from file)
  * If we do have the "use specification to create files" feature, then it would
    make a lot more sense to allow sizes to be specified.  This could
    potentially break the feature of "include the path as the content".  But if
    the user doesn't need to create very small files, we could still use the
    relative path as the first line of the file, and then fill it up with zeroes
    after that.  The reporting tool would know to only look at the first line.
* use file [magic numbers](https://en.wikipedia.org/wiki/List_of_file_signatures)
  * Above, we used the example of an `mp3` file, whose content was entirely
    text.  In some contexts, it could be more useful to make it look more like
    an actual `mp3` file.  Some programs use the "magic number" at the beginning
    of the file to identify its type.  We could copy the first _N_ bytes from
    the original file, and then include the relative path after that.  We'd have
    to adjust the reporting mechanism to know to skip ahead _N_ bytes in order
    to find the original location.  This might also complicate looking at the
    contents of the file with tools like `cat` or simple text editors.
