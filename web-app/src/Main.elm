module Main exposing (..)

import Browser
import Model exposing (..)
import Ports exposing (..)
import Subscriptions exposing (..)
import Update exposing (..)
import View exposing (..)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        }