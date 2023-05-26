Trainder

## Set up your dev env 
- Install Java (I know. Sorry.)

- Install leiningen:
```
$ brew install lein
``` 

- Install all deps
```
$ npm ci 
```

- Start shadow-cljs
```
$ npx shadow-cljs watch app
;; Wait for first compile to finish or expo gets confused
```

- (In another terminal) Start expo
```
$ npx expo start 
```

